package com.examstack.portal.controller.restful;


import com.examstack.common.domain.exam.Exam;
import com.examstack.common.domain.exam.ExamHistory;
import com.examstack.common.util.StandardPasswordEncoderForSha1;
import com.examstack.portal.security.UserDetailsServiceImpl2;
import com.examstack.portal.security.UserInfo;
import com.examstack.portal.service.ExamService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;

/**
 * Created by suzengshuai on 2018/4/19.
 */
@RestController
@RequestMapping("/")
public class LoginService {
    @Autowired
    public ExamService examService;

    @Autowired
    protected HttpServletRequest request;

    @Autowired
    private UserDetailsServiceImpl2 userDetailsService;

    @RequestMapping(value = "/login/{username}/{password}", method = RequestMethod.GET)
    public @ResponseBody
    String login(@PathVariable String username, @PathVariable String password) {

        //加密
        String sh1Password = password + "{" + username + "}";
        PasswordEncoder passwordEncoder = new StandardPasswordEncoderForSha1();
        String result = passwordEncoder.encode(sh1Password);

        if (!"".equals(username) && "".equals(password))
            throw new AuthenticationServiceException("请输入密码！");
//        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);


        //根据用户名username加载userDetails
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        //根据userDetails构建新的Authentication,这里使用了
        //PreAuthenticatedAuthenticationToken当然可以用其他token,如UsernamePasswordAuthenticationToken
        PreAuthenticatedAuthenticationToken authentication =
                new PreAuthenticatedAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());

        //设置authentication中details
        authentication.setDetails(new WebAuthenticationDetails(request));

        //存放authentication到SecurityContextHolder
        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = request.getSession(true);
        //在session中存放security context,方便同一个session中控制用户的其他操作
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        Gson gson = new Gson();

        return null;
    }
}
