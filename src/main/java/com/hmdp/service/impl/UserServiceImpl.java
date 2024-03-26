package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

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
}
