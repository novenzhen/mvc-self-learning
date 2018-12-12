package org.learning.mvc.sevlet.demo;

import org.learning.mvc.sevlet.annocation.NService;

/**
 * @author : noven.zhen
 * @date : 2018-12-11
 * @email: zjm@choicesoft.com.cn
 */
@NService
public class DemoserviceImpl implements IDemoservice {

    @Override
    public String get(String name) {
        return "Service " + name;
    }
}
