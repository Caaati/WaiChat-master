// src/main/java/com/zafu/waichat/service/ChatService.java
package com.zafu.waichat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zafu.waichat.pojo.dto.UserContactDTO;
import com.zafu.waichat.pojo.entity.Chat;

import java.util.HashMap;
import java.util.List;

public interface ChatService extends IService<Chat> {
    List<Chat> getChatHistory(Long userId1, Long userId2);

    boolean saveChatMessage(Chat chat);

    List<UserContactDTO> getContactList(Long userId);
}