package org.learning.mvc.sevlet;

import org.learning.mvc.sevlet.annocation.NAutowired;
import org.learning.mvc.sevlet.annocation.NController;
import org.learning.mvc.sevlet.annocation.NRequestMapping;
import org.learning.mvc.sevlet.annocation.NService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author : noven.zhen
 * @date : 2018-12-11
 * @email: zjm@choicesoft.com.cn
 */
public class DispatchServlet extends HttpServlet {
    private Properties config=new Properties();
    private List<String> classNames=new ArrayList<>();
    private Map<String,Object> ioc=new HashMap<>();
    private Map<String,Method> handlerMapping=new HashMap<>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException{
        try {
            doDispatch(req,resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doDispatch(HttpServletRequest req,HttpServletResponse response) throws IOException {
        String uri=req.getRequestURI();
        String contextPath=req.getContextPath();
        uri=uri.replace(contextPath,"").replaceAll("/+","/");
        if(!handlerMapping.containsKey(uri)){
            response.getWriter().write("Not found");
            return;
        }
        Map<String,String[]> params=req.getParameterMap();
        Method method=handlerMapping.get(uri);
        String beanName=lowerFirstCase(method.getDeclaringClass().getSimpleName());
        Object instance=ioc.get(beanName);
        try {
            method.invoke(instance,new Object[]{req,response,params.get("name")[0]});
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        //1.加载配置文件
        doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));

        //2.解析配置文件，并且读取信息，完成扫描scanPackage
        doScanne(config.getProperty("scanPackage"));

        //3.初始化扫描的所有的类，并且放入ioc容器中
        doInstance();

        //4.完成自动化注入
        doAutowired();

        //5.初始化handlermapping，url和method进行1对1关联
        initHandlerMapping();

        System.out.println("noven learning mvc is inited!");
    }

    private void initHandlerMapping() {

        if(ioc.isEmpty()){return;}
        for(Map.Entry entry:ioc.entrySet()){
            Class<?> clazz=entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(NController.class)){continue;}
            String baseUrl="";
            if(clazz.isAnnotationPresent(NRequestMapping.class)){
                NRequestMapping requestMapping=clazz.getAnnotation(NRequestMapping.class);
                baseUrl=requestMapping.value();
            }
            Method[] methods=clazz.getMethods();
            for (Method method : methods) {
                if(!method.isAnnotationPresent(NRequestMapping.class)){continue;}
                NRequestMapping requestMapping=method.getAnnotation(NRequestMapping.class);
                String url=(baseUrl+"/"+requestMapping.value()).replaceAll("/+","/");
                handlerMapping.put(url,method);
                System.out.println("Mapped:"+url+","+method);
            }
        }
    }

    private void doAutowired() {
        if(ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String,Object> entry:ioc.entrySet()) {
            Field[] fields=entry.getValue().getClass().getDeclaredFields();
            for(Field f:fields){
                if(!f.isAnnotationPresent(NAutowired.class)){continue;}
                NAutowired autowired=f.getAnnotation(NAutowired.class);
                String beanName=autowired.value().trim();
                if("".equals(beanName)){
                    beanName=f.getType().getSimpleName();
                }
                f.setAccessible(true);
                try {
                    Object object=ioc.get(beanName);
                    f.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

    }

    private void doInstance() {

        if(classNames.isEmpty()){return;}

            try {
                for (String className:classNames) {
                Class<?> clazz=Class.forName(className);
                if(clazz.isAnnotationPresent(NController.class)){
                    String beanName=lowerFirstCase(clazz.getSimpleName());
                    if(ioc.containsKey(beanName)){
                        throw new Exception("the beanname is defined");
                    }
                    ioc.put(beanName,clazz.newInstance());
                }
                else if(clazz.isAnnotationPresent(NService.class)){

                    NService service=clazz.getAnnotation(NService.class);
                    String beanName=service.value();
                    if("".equals(beanName.trim())){
                        beanName=lowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance=clazz.newInstance();
                    if(ioc.containsKey(beanName)){
                        throw new Exception("the beanname is defined");
                    }
                    ioc.put(beanName,instance);
                    Class<?>[] interfaces=clazz.getInterfaces();
                    for(Class<?> i:interfaces){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("the beanname is defined");
                        }
                        ioc.put(i.getSimpleName(),instance);
                    }
                }else{
                    continue;
                }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private String lowerFirstCase(String simpleName) {
        char[] chars=simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanne(String scanPackage) {
        URL url=this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classDir=new File(url.getFile());
        for(File file:classDir.listFiles()){
            if(file.isDirectory()){
                doScanne(scanPackage+"."+file.getName());
            }else{
                String className=(scanPackage+"."+file.getName().replace(".class",""));
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {
        InputStream is=this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            config.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(null!=is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
