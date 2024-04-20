package com.dp.controller;


import com.dp.dto.Result;
import com.dp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 */
@RestController
@RequestMapping("/follow")
public class FollowController {



    @Resource
    private IFollowService followService;

    /**
     * 关注与取关
     * @param id
     * @param isFollow
     * @return
     */

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long id,@PathVariable("isFollow") Boolean isFollow)
    {
        return followService.follow(id,isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result followOrNot(@PathVariable("id") Long id)
    {
        return followService.isFollow(id);
    }

    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id)
    {

        return followService.followCommons(id);

    }

}
