package org.learning.mvc.sevlet.annocation;

import java.lang.annotation.*;

/**
 * @author : noven.zhen
 * @date : 2018-12-11
 * @email: zjm@choicesoft.com.cn
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NAutowired {
    String value() default "";
}
