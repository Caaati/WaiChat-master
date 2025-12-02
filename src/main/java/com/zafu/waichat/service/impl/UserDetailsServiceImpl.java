package com.zafu.waichat.service.impl;// com.zafu.waichat.service.UserDetailsServiceImpl
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zafu.waichat.mapper.UserMapper;
import com.zafu.waichat.pojo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查询数据库
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));

        // 如果用户不存在，抛出异常
        if (user == null) {
            throw new UsernameNotFoundException("用户名不存在");
        }

        // 构建 Spring Security 所需的 UserDetails 对象
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword()) // 数据库中已加密的密码
                .roles("USER") // 角色（必须以 "ROLE_" 开头）
                .build();
    }
}