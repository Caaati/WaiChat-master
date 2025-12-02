package com.zafu.waichat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zafu.waichat.mapper.UserMapper;
import com.zafu.waichat.pojo.dto.UserContactDTO;
import com.zafu.waichat.pojo.entity.User;
import com.zafu.waichat.util.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@Slf4j
@Api(tags = "用户相关接口")
public class UserController {
    @Autowired
    private UserMapper userMapper;

    @RequestMapping("/search")
    public Result searchUser(@RequestParam String key) {
        log.info("搜索用户【{}】", key);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.like("username", key).or().like("nickname", key).or().eq("id", key);
        List<User> userList = userMapper.selectList(wrapper);
        List<UserContactDTO> collect = userList.stream().map(user -> {
            UserContactDTO userContactDTO = new UserContactDTO();
            userContactDTO.setId(user.getId());
            userContactDTO.setUsername(user.getUsername());
            userContactDTO.setNickname(user.getNickname());
            return userContactDTO;
        }).collect(Collectors.toList());
        log.info("搜索结果：{}", collect);
        if (collect.size() == 0)
            return Result.error("无搜索结果");
        return Result.success(collect);
    }
}
