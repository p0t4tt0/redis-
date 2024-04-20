package com.dp.service;

import com.dp.dto.Result;
import com.dp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result queryHotBlog(Integer current);

    Result likeBlog(Long id);

    Result queryBlogLikesById(Long id);

    Result savrBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}
