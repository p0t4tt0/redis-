package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dp.dto.Result;
import com.dp.dto.UserDTO;
import com.dp.entity.Follow;
import com.dp.mapper.FollowMapper;
import com.dp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.service.IUserService;
import com.dp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long id, Boolean isFollow) {

        Long userId = UserHolder.getUser().getId();
        String key= "follows:"+userId;

        //判断关注和取关
        if(isFollow) {
            //关注，新增数据库
            Follow follow=new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            follow.setCreateTime(LocalDateTime.now());

            boolean isSuccess = save(follow);

            if(isSuccess)
            {
                //将关注的人放入redis集合，实现共共同关注交集
                stringRedisTemplate.opsForSet().add(key,String.valueOf(id));


            }

        }
        else {
            //取关，删除数据库

            boolean isSuccess = remove(new QueryWrapper<Follow>().eq("user_id", userId)
                    .eq("follow_user_id", id));

            if(isSuccess) {
                stringRedisTemplate.opsForSet().remove(key, String.valueOf(id));
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long id) {

        UserDTO user = UserHolder.getUser();

        Integer count = query().eq("user_id", user.getId()).eq("follow_user_id", id).count();

        //判断是否关注

            return Result.ok(count>0);

    }

    @Override
    public Result followCommons(Long id) {

        //获取当前用户
        Long userId = UserHolder.getUser().getId();

        String key1="follows:"+userId;
        String key2="follows:"+id;
        //求交集
        Set<String> common = stringRedisTemplate.opsForSet().intersect(key1, key2);

        if(common==null||common.isEmpty())
        {
            return null;
        }

        //解析id
        List<Long> ids = common.stream().map(Long::valueOf).collect(Collectors.toList());

        //查询用户
        List<UserDTO> userDTOS = userService.listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());


        return Result.ok(userDTOS);
    }
}
