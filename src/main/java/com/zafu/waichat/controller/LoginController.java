package com.zafu.waichat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zafu.waichat.mapper.UserMapper;
import com.zafu.waichat.pojo.entity.User;
import com.zafu.waichat.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class LoginController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 注册功能
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, user.getUsername());
        User existingUser = userMapper.selectOne(queryWrapper);

        if (existingUser != null) {
            // 用户名已存在，返回错误信息
            return Result.error("用户名已被占用，请更换其他用户名");
        }

        // 2. 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 设置默认昵称
        user.setNickname(user.getUsername());
        // 3. 保存用户信息
        userMapper.insert(user);
        return Result.success();
    }

}