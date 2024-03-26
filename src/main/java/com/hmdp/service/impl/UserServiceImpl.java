package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    /**
     * 发售手机验证码
     * @param phone
     * @param session
     * @return
     */
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号

        if(RegexUtils.isPhoneInvalid(phone))
        {
            //不符合，返回错误信息
            return Result.fail("手机号不合法！");
        }





        //符合，生成验证码
        String validcode = RandomUtil.randomNumbers(6);//hutool工具包生成六位随机数验证码
        //保存验证码到session

        session.setAttribute("code",validcode);
        //TODO 发送验证码

        log.debug("发送短信验证码成功：{}",validcode);

        return Result.ok();
    }

    /**
     * 用户登录
     * @param loginForm
     * @param session
     * @return
     */
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        //校验手机号


        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //不符合，返回错误信息
            return Result.fail("手机号不合法！");
        }
        // 验证码

        Object cacheCode = session.getAttribute("code");
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            //不一致，报错
            return Result.fail("验证码错误");

        }


        //一致，查询用户,select* from tb_user where phone=?
        User user = query().eq("phone", phone).one();//mybatisplus查询
        //判断是否存在用户

        if(user==null)
        {
            //不存在，创建新用户并保存
           user= createUserWithPhone(phone);
        }




        //保存用户信息到session

        session.setAttribute("user",user);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {

        //创建用户
        User user=new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));

        //保存用户
        save(user);

        return user;
    }
}
