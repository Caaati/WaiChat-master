package com.zafu.waichat.controller;

import com.zafu.waichat.pojo.dto.UserContactDTO;
import com.zafu.waichat.websocket.WebSocket;
import com.zafu.waichat.mapper.ChatMapper;
import com.zafu.waichat.pojo.entity.Chat;
import com.zafu.waichat.service.ChatService;
import com.zafu.waichat.util.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/chat")
@Slf4j
@Api(tags = "聊天相关接口")
public class ChatController {
    @Autowired
    private WebSocket webSocket;
    @Autowired
    private ChatMapper chatMapper;
    @Autowired
    private ChatService chatService;

    @PostMapping("/send")
    public Result sendMessage(@RequestBody Chat chat) {
        try {
            // 由WebSocket统一处理保存和发送，这里不再重复保存
            webSocket.sendOneMessage(chat);
            return Result.success();
        } catch (Exception e) {
            // 处理异常（包括用户不在线的情况）
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/sendall")
    public Result testMessage(@RequestParam String message) {
        webSocket.sendAllMessage(message);
        return Result.success();
    }

    @GetMapping("/history")
    public Result getChatHistory(
            @RequestParam Long userId,
            @RequestParam Long targetId) {
        List<Chat> history = chatService.getChatHistory(userId, targetId);
        return Result.success(history);
    }

    @GetMapping("/getContactList")
    public Result getContactList(
            @RequestParam Long userId) {
        List<UserContactDTO> contacts = chatService.getContactList(userId);
        return Result.success(contacts);
    }
}
