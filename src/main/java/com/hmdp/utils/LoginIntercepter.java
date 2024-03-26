package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginIntercepter implements HandlerInterceptor {

    /**
     * 进入controller之前
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //获取session

        HttpSession session = request.getSession();

        //获取session中用户

        User user = (User) session.getAttribute("user");

        //判断用户是否存在

        if(user==null) {
            //不存在，拦截
            response.setStatus(401);

            return false;
        }

        //存在，将用户信息存放在threadlocal

        UserDTO userDTO=new UserDTO();

        userDTO.setNickName(user.getNickName());
        userDTO.setId(user.getId());
        userDTO.setIcon(user.getIcon());


        UserHolder.saveUser(userDTO);



        //放行
        return true;
    }

    /**
     * 业务完成之后销毁用户信息，避免内存泄漏
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        //移除user
        UserHolder.removeUser();
    }
}
