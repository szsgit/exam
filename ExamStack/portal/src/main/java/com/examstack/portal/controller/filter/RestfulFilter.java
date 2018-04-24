package com.examstack.portal.controller.filter;

import com.examstack.portal.security.UserInfo;
import com.google.gson.*;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by suzengshuai on 2018/4/17.
 */
public class RestfulFilter implements HandlerInterceptor {
    private Logger logger = LogManager.getLogger(RestfulFilter.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        response.addHeader("Access-Control-Allow-Origin","*");
        response.addHeader("Access-Control-Allow-Methods","*");
        response.addHeader("Access-Control-Max-Age","100");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
        response.addHeader("Access-Control-Allow-Credentials","false");

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, ModelAndView modelAndView) throws Exception {
        if(null == modelAndView){
            return;
        }
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("status","true");
        jsonObj.addProperty("message","");

        String datatype = request.getParameter("datatype");
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        Map<String,Object> data = modelAndView.getModel();
        String jsonstr = gson.toJson(data);

        String rgex = "<json>(.*?)</json>";
        List<String> jsonStrls = getSubUtil(jsonstr,rgex);



        if("json".equals(datatype)){
            if(null == jsonStrls || jsonStrls.size() <1){
                jsonObj.add("data", new JsonParser().parse(jsonstr).getAsJsonObject());
            }else {

                JsonArray jsonArray = new JsonArray();
                for (String questr : jsonStrls
                        ) {
                    questr = questr.replace("\\\"", "\"").replace("\\\"", "\"");
                    JsonObject qeobj = new JsonParser().parse(questr).getAsJsonObject();
                    jsonArray.add(qeobj);
                }

                jsonObj.add("data", jsonArray);
            }





            System.out.printf("---------返回JSON");
            response.reset();
//            String str = deleteAllHTMLTag(jsonObj.toString());
            String str = jsonObj.toString();
            returnJson(response,str);
        }else{
            if(null == jsonStrls || jsonStrls.size() <1) return;

            final String regex = "<json>(.*?)</json>";
            String str = modelAndView.getModel().get("questionStr").toString().replaceAll(regex,"");

            modelAndView.getModel().put("questionStr",str);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws Exception {

    }

    /**
     * 删除所有的HTML标签
     *
     * @param source 需要进行除HTML的文本
     * @return
     */
    public static String deleteAllHTMLTag(String source) {

        if(source == null) {
            return "";
        }

        String s = source;
        /** 删除普通标签  */
        s = s.replaceAll("<(S*?)[^>]*>.*?|<.*? />", "");
        /** 删除转义字符 */
        s = s.replaceAll("&.{2,6}?;", "");
        return s;
    }

    /**
     * 正则表达式匹配两个指定字符串中间的内容
     * @param soap
     * @return
     */
    public List<String> getSubUtil(String soap,String rgex){
        List<String> list = new ArrayList<String>();
        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            int i = 1;
            list.add(m.group(i));
            i++;
        }
        return list;
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
