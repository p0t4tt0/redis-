package com.hmdp.config;

import com.hmdp.utils.LoginIntercepter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MVCConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginIntercepter())
        .excludePathPatterns(
                "shop-type/**",
                "upload/**",
                "voucher/**",
                "shop/**",
                "blog/hot",
                "/user/code",
                "/user/login"
        );//放行一部分功能
    }
}
