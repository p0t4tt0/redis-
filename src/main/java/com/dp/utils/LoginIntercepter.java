package com.dp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginIntercepter implements HandlerInterceptor {


    /**
     * 进入controller之前,登录校验
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

     //判断是否需要拦截

        if (UserHolder.getUser()==null) {
            //没有，拦截
            response.setStatus(401);

            return false;
        }
        return true;
    }


}
