package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dp.dto.Result;
import com.dp.dto.ScrollResult;
import com.dp.dto.UserDTO;
import com.dp.entity.Blog;
import com.dp.entity.Follow;
import com.dp.entity.User;
import com.dp.mapper.BlogMapper;
import com.dp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.service.IFollowService;
import com.dp.service.IUserService;
import com.dp.utils.RedisConstants;
import com.dp.utils.SystemConstants;
import com.dp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;
    @Override
    public Result queryBlogById(Long id) {
        //查询blog
        Blog blog = getById(id);
        if (blog==null) {
            return Result.fail("笔记不存在！");
        }
        //查询blog用户
        queryBlogUser(blog);
        //查询是否点过赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {

        //获取用户
        UserDTO user = UserHolder.getUser();
        if(user==null)
        {
            return;
        }
        Long userId =user.getId() ;
        //判断当前用户是否点赞
        String key= RedisConstants.BLOG_LIKED_KEY+blog.getId();
        Double isliked = stringRedisTemplate.opsForZSet().score(key, String.valueOf(userId));

        blog.setIsLike(isliked!=null);


    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);

        });
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {

        //获取用户
        Long userId = UserHolder.getUser().getId();
        //判断当前用户是否点赞
        String key= RedisConstants.BLOG_LIKED_KEY+id;
        Double score = stringRedisTemplate.opsForZSet().score(key, String.valueOf(userId));
        //判断
        if (score==null) {
            //未点赞--数据库加一，redis保存

            boolean isSucess = update().setSql("liked=liked+1").eq("id", id).update();
            if (isSucess) {
                stringRedisTemplate.opsForZSet().add(key,String.valueOf(userId),System.currentTimeMillis());
            }

        }
        else {


            //已点赞--数据库减一，redis删除

            boolean isSucess = update().setSql("liked=liked-1").eq("id", id).update();
            if (isSucess) {
                stringRedisTemplate.opsForZSet().remove(key,String.valueOf(userId));
            }

        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikesById(Long id) {

        //查询top5点赞用户
        String key= RedisConstants.BLOG_LIKED_KEY+id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);

        if(top5==null||top5.isEmpty())
        {
            return Result.ok(Collections.emptyList());
        }

        //解析用户id
        List<Long> ids = null;



            ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());


        //查询用户

        String idstr= StrUtil.join(",",ids);

        List<UserDTO> users = userService.query()
                .in("id",ids)
                .last("order by field(id,"+idstr+")")
                .list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }

    @Override
    public Result savrBlog(Blog blog) {
        //获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());

        //保存探店笔记
        boolean issuccess = save(blog);

        if(!issuccess) {

            return Result.fail("新增笔记失败！");
        }

            //查询所有粉丝
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();

        //发送给粉丝

        for(Follow follow:follows)
        {

            //获取粉丝id
            Long userId = follow.getUserId();

            //推送
            String key="feed:"+userId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }



        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //获取当前用户

        Long userId = UserHolder.getUser().getId();


        //查询收件箱
        String key=RedisConstants.FEED_KEY+userId;

        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);

        if(typedTuples==null||typedTuples.isEmpty())
        {
            return Result.ok();
        }



        //解析数据blogId，minTime、offset

        List<Long>ids=new ArrayList<>(typedTuples.size());

        long minTime=0;


        int os=1;

        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            //id

            ids.add(Long.valueOf(typedTuple.getValue()));
            //score
            long time= typedTuple.getScore().longValue();

            if(time==minTime)
            {
                os++;
            }
            else{
                minTime=time;
                os=1;
            }
        }

        //根据id查blog

        String idstr= StrUtil.join(",",ids);
        List<Blog> blogs= query().in("id", ids)
                .last("order by field(id," + idstr + ")")
                .list();

        for (Blog blog : blogs) {
            //查询blog用户
            queryBlogUser(blog);
            //查询是否点过赞
            isBlogLiked(blog);

        }


        //封装返回

        ScrollResult result=new ScrollResult();
        result.setList(blogs);
        result.setMinTime(minTime);
        result.setOffset(os);
        return Result.ok(result);
    }
}
