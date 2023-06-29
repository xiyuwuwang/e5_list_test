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

    }
