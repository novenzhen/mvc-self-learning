package org.learning.mvc.sevlet.demo;

import org.learning.mvc.sevlet.annocation.NAutowired;
import org.learning.mvc.sevlet.annocation.NController;
import org.learning.mvc.sevlet.annocation.NRequestMapping;
import org.learning.mvc.sevlet.annocation.NRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author : noven.zhen
 * @date : 2018-12-11
 * @email: zjm@choicesoft.com.cn
 */
@NController
@NRequestMapping("/demo")
public class DemoAction {

    @NAutowired
    IDemoservice iDemoservice;

    @NRequestMapping("/query.json")
    public  void query(HttpServletRequest req, HttpServletResponse rep, @NRequestParam("name") String name){
        String result=iDemoservice.get(name);
        try {
            rep.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
