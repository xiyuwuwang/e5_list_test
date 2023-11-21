import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.disclosure.common.core.domain.entity.SysDept;
import com.disclosure.common.core.domain.entity.SysRole;
import com.disclosure.common.core.domain.entity.SysUser;
import com.disclosure.common.core.domain.model.LoginUser;
import com.disclosure.common.exception.ServiceException;
import com.disclosure.common.utils.SecurityUtils;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;

import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import static com.disclosure.business.config.InitiateInterceptField.*;

@Slf4j
@AllArgsConstructor
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class AuthorityUserFillerInterceptor implements Interceptor {

    private final Cache<Long, SysDept> systemDeptMap;
    private final Cache<Long, Set<Long>> systemRoleDeptMap;
    @Override
    @SneakyThrows
    public Object intercept(Invocation invocation) {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameterObjectY = args[1];
    
        //限制SQL类型
        if (!SqlCommandType.UPDATE.equals(mappedStatement.getSqlCommandType())
                && !SqlCommandType.INSERT.equals(mappedStatement.getSqlCommandType())
        ) {
            return invocation.proceed();
        }
        BoundSql boundSql = mappedStatement.getBoundSql(parameterObjectY);
        Configuration configuration = mappedStatement.getConfiguration();
        //获取用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser currentUser;
        if (ObjectUtil.isNull(loginUser) || ObjectUtil.isNull(currentUser = loginUser.getUser()))
            throw new ServiceException("获取不到用户数据");

        String originalSql = boundSql.getSql();
        Statement statement = CCJSqlParserUtil.parse(new StringReader(originalSql));
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (statement instanceof Update) {
            Update update = (Update) CCJSqlParserUtil.parse(originalSql);
            ArrayList<UpdateSet> updateSets = update.getUpdateSets();
            updateData(updateSets ,configuration, parameterMappings, parameterObjectY, deptId, currentUser.getDeptId(), currentUser);
            updateData(updateSets ,configuration, parameterMappings, parameterObjectY, updateBy, String.valueOf(currentUser.getUserId()), currentUser);
            originalSql = update.toString();

        } else if (statement instanceof Insert) {
            Insert insert = (Insert) CCJSqlParserUtil.parse(originalSql);
            List<Column> columns = insert.getColumns();
            List<Expression> expressions = insert.getItemsList(ExpressionList.class).getExpressions();
            //部门权限问题
            insertData(columns, expressions, configuration, parameterMappings, parameterObjectY, deptId, currentUser.getDeptId(), currentUser);
            insertData(columns, expressions, configuration, parameterMappings, parameterObjectY, createBy, String.valueOf(currentUser.getUserId()), currentUser);
            originalSql = insert.toString();
        } else {
            return invocation.proceed();
        }
        MetaObject metaObject = SystemMetaObject.forObject(boundSql);
        metaObject.setValue("sql", originalSql);

        return invocation.proceed();
    }

    private void updateData(ArrayList<UpdateSet> updateSets, Configuration configuration, List<ParameterMapping> parameterMappings,
                            Object parameterObject, String key, Object value, SysUser currentUser) {

        MetaObject metaObject = SystemMetaObject.forObject(parameterObject);
        Object substitution = metaObject.getValue(key);
        if (substitution == null) {
            if (key.equals(deptId)) {
                return;
            }
            updateSets.add(new UpdateSet() {{
                add(new Column(StrUtil.toUnderlineCase(key)), new JdbcParameter());
            }});
            parameterMappings.add(new ParameterMapping.Builder(configuration, key, value.getClass()).build());
        } else if (deptId.equals(key)) {
            departmentalPermissions(currentUser, (Long) substitution);
            return;
        }
        metaObject.setValue(key, value);


    }

    private void insertData(List<Column> columns, List<Expression> expressions, Configuration configuration, List<ParameterMapping> parameterMappings,
                            Object parameterObject, String key, Object value, SysUser currentUser) {
		//获取元数据
        MetaObject metaObject = SystemMetaObject.forObject(parameterObject);
        Object substitution = metaObject.getValue(key);
          //判断修改字段是否存在
        if (substitution == null) {
        //添加插入字段
            columns.add(new Column(StrUtil.toUnderlineCase(key)));
            //补充?占位符
            expressions.add(new JdbcParameter());
           // 添加parameterMapping映射
            parameterMappings.add(new ParameterMapping.Builder(configuration, key, value.getClass()).build());
        } else if (deptId.equals(key)) {
            departmentalPermissions(currentUser, (Long) substitution);
            return;
        }
        //往元数据插入新值
        metaObject.setValue(key, value);


    }

//用作部门权限判断可不用
    private void departmentalPermissions(SysUser currentUser, Long deptId) {
        Long currentUserDeptId = currentUser.getDeptId();
        if (deptId.equals(currentUserDeptId)) {
            return;
        }
        SysDept sysDept = systemDeptMap.getIfPresent(deptId);
        if (sysDept == null) {
            throw new ServiceException("部门不存在");
        }
        if (Arrays.asList(sysDept.getAncestors().split(",")).contains(currentUserDeptId.toString())) {
            return;
        }
        boolean contains = currentUser.getRoles().parallelStream().map(SysRole::getRoleId).map(systemRoleDeptMap::getIfPresent)
                .filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toSet()).contains(deptId);
        if (contains) {
            return;
        }
        throw new ServiceException("权限不足,无法(插入/修改)当前部门");

    }

    /**
     * 生成拦截对象的代理
     *
     * @param target 目标对象
     * @return 代理对象
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * mybatis配置的属性
     *
     * @param properties mybatis配置的属性
     */
    @Override
    public void setProperties(Properties properties) {

    }
