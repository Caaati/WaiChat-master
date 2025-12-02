// src/main/java/com/zafu/waichat/config/LoginSuccessHandler.java
package com.zafu.waichat.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zafu.waichat.mapper.UserMapper;
import com.zafu.waichat.pojo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // 1. 从 Authentication 对象中获取认证成功的用户信息
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        // 2. 根据用户名从数据库中查询出完整的用户对象（包含 userId）
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));

        // 3. 构建要返回给前端的响应数据
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("code", 1);
        responseData.put("msg", "登录成功");

        // 将完整的用户信息（或仅需要的字段）放入 data 中
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());         // 返回 userId
        userData.put("username", user.getUsername()); // 返回用户名
        userData.put("nickname", user.getNickname()); // 如果有昵称也返回

        responseData.put("data", userData);

        // 4. 设置响应头和响应体
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(objectMapper.writeValueAsString(responseData));
    }
}