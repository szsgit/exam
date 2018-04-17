package com.examstack.portal.controller.filter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Created by suzengshuai on 2018/4/17.
 */
public class RestfulFilter implements HandlerInterceptor {
    private Logger logger = LogManager.getLogger(RestfulFilter.class);

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, ModelAndView modelAndView) throws Exception {
        String datatype = request.getParameter("datatype");
        Gson gson = new Gson();

        if("json".equals(datatype)){
            Map<String,Object> data = modelAndView.getModel();
            JsonObject jsonObj = new JsonObject();
            jsonObj.addProperty("status","true");
            jsonObj.addProperty("message","");
            jsonObj.addProperty("data",gson.toJson(data));


            System.out.printf("---------返回JSON");
            response.reset();
            returnJson(response,jsonObj.toString());
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws Exception {

    }

    private void returnJson(HttpServletResponse response, String json) throws Exception{
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try {
            writer = response.getWriter();
            writer.print(json);

        } catch (IOException e) {
            logger.error("response error",e);
        } finally {
            if (writer != null)
                writer.close();
        }
    }
}
