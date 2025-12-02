package com.zafu.waichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zafu.waichat.pojo.dto.UserContactDTO;
import com.zafu.waichat.pojo.entity.Chat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface ChatMapper extends BaseMapper<Chat> {
    List<Chat> selectChatHistory(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    List<UserContactDTO> selectContactList(Long userId);
}
