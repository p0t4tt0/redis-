package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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


    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
        //保存验证码到redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY +phone,validcode,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);//有效期两分钟
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
        // 验证码,从redis获取

        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
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

        UserDTO userDTO=new UserDTO();

        BeanUtils.copyProperties(user,userDTO);



        //保存用户信息到redis



        //生成登陆令牌token

        String token = UUID.randomUUID().toString(true);//不带中划线的uuid

        //user转hash

        BeanUtils.copyProperties(user,userDTO);
        Map<String, Object> userDTOMap = BeanUtil.beanToMap(userDTO);

        String id = userDTOMap.get("id").toString();
        userDTOMap.put("id",id);

        //保存

        String tokenKey=RedisConstants.LOGIN_USER_KEY+token+"";
        stringRedisTemplate.opsForHash().putAll(tokenKey,userDTOMap);
        //设置有效期
        stringRedisTemplate.expire(tokenKey,30,TimeUnit.MINUTES);

        //返回token


        return Result.ok(token);
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
