import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.regex.Pattern;

@Slf4j
public class TestBt {

    @Test
    public void test8848() {
        String s = "^[\\-\\+]?(0(\\.\\d{1,8})?|([1-9](\\d)?)(\\.\\d{1,8})?|1[0-7]\\d{1}(\\.\\d{1,8})?|180(([.][0]{1,8})?))$";

        boolean matches = Pattern.matches(s, "120.6811124");
        System.out.println(matches);        
    }
        //获取某个时间段每天的日期
    @Test
    public void test01() {
    LocalDateTime min = LocalDateTime.now().with(LocalTime.MIN);
    LocalDateTime max = LocalDateTime.now().plusDays(10).with(LocalTime.MAX);
   List<LocalDateTime> collect = Stream.iterate(min, localDate -> localDate.plusDays(1))
            .limit(ChronoUnit.DAYS.between(min, max) + 1)
            .collect(Collectors.toList());
    }

    @Test
    public void test02() {
  //直接去重
List<RealNameUploadRecord> collect = 
clockingVO.stream().distinct().collect(Collectors.toList());

    }
    

    }
