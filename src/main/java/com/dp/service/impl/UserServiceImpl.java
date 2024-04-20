package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.LoginFormDTO;
import com.dp.dto.Result;
import com.dp.dto.UserDTO;
import com.dp.entity.User;
import com.dp.mapper.UserMapper;
import com.dp.service.IUserService;
import com.dp.utils.RedisConstants;
import com.dp.utils.RegexUtils;
import com.dp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

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

        return Result.ok(validcode);
    }

    /**
     * 用户登录
     * @param loginForm
     * @param session
     * @return
     */
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        //校验手机号


        log.info("登录");
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

    @Override
    public Result sign() {


        //获取当前登录用户

        Long userId = UserHolder.getUser().getId();


        //获取日期

        LocalDateTime now = LocalDateTime.now();

        String date = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));


        //拼接key用于bitmap
        String key=RedisConstants.USER_SIGN_KEY+userId+date;

        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();



        //写入redis setbit key offset 1
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);

        return Result.ok();
    }

    @Override
    public Result signCount() {

        //获取当前登录用户

        Long userId = UserHolder.getUser().getId();


        //获取日期

        LocalDateTime now = LocalDateTime.now();

        String date = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));


        //拼接key用于bitmap
        String key = RedisConstants.USER_SIGN_KEY + userId + date;

        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();

        //获取截至今天为止的所有签到记录

        List<Long> bitField = stringRedisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));

        if (bitField == null || bitField.isEmpty()) {
            return Result.ok(0);
        }

        Long num = bitField.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }

        //循环遍历
        int count=0;
        while (true)
        {
            //让当前数字与1做与运算，判断是否为零
            if((num&1)==0)
            {//为零，未签到结束
                break;
            }
            else {
                count++;

            }
            //数字右移一位，继续循环
            num>>>=1;//右移一位
        }




        return Result.ok(count);
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
