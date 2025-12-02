package com.zafu.waichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zafu.waichat.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}