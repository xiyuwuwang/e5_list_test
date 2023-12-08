import com.disclosure.business.Importing.service.ImportingData;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
/**
*获取多态接口
    **/

@Component
public class ApplicationContextFactory implements ApplicationContextAware {

    private final Map<Class<?>, ImportingData<Object>> importingDataService = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        applicationContext.getBeansOfType(ImportingData.class)
                .forEach((k, v) -> importingDataService.put(GenericTypeResolver.resolveTypeArgument(v.getClass(), ImportingData.class), v));
    }

    public <T> ImportingData<Object> getImportingDataService(Class<T> c) {
        return importingDataService.get(c);

    }


}
