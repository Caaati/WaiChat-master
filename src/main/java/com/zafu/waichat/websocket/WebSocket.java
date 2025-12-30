package com.zafu.waichat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zafu.waichat.pojo.entity.Chat;
import com.zafu.waichat.service.ChatService;
import com.zafu.waichat.util.Result;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
@ServerEndpoint(value = "/ws/{userId}")
public class WebSocket {
    // 1. 将成员变量改为 static
    private static ObjectMapper objectMapper;
    private static ChatService chatService;

    // 2. 通过 setter 方法配合 @Autowired 注入静态变量
    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        WebSocket.objectMapper = objectMapper;
    }

    @Autowired
    public void setChatService(ChatService chatService) {
        WebSocket.chatService = chatService;
    }
    private Session session;
    /**
     * 用户ID
     */
    private Integer userId;
    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    private static CopyOnWriteArraySet<WebSocket> webSockets = new CopyOnWriteArraySet<>();
    // 用来存在线连接用户信息
    private static ConcurrentHashMap<Integer, Session> sessionPool = new ConcurrentHashMap<Integer, Session>();

    /**
     * 链接成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Integer userId) {
        try {
            this.session = session;
            this.userId = userId;
            webSockets.add(this);
            sessionPool.put(userId, session);
            log.info("【WebSocket】有新的连接【{}】，总数为:{}", userId, webSockets.size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 链接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        try {
            webSockets.remove(this);
            sessionPool.remove(this.userId);
            log.info("【WebSocket】连接断开【{}】，总数为:{}", userId, webSockets.size());
        } catch (Exception e) {
        }
    }

    /**
     * 收到客户端消息后调用的方法
     * 优化点：处理客户端发送的消息并转发
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("【WebSocket】收到来自【{}】的消息:{}", userId, message);
        try {
            // 解析客户端发送的消息
            Chat chat = objectMapper.readValue(message, Chat.class);
            // 补充发送者ID（从连接参数获取，防止伪造）
            chat.setUserId(this.userId);
            // 确保创建时间正确设置
            if (chat.getCreateTime() == null) {
                chat.setCreateTime(LocalDateTime.now());
            }
            // 转发消息给目标用户
            sendOneMessage(chat);
        } catch (Exception e) {
            log.error("消息处理失败", e);
            try {
                // 向发送者返回错误信息
                session.getBasicRemote().sendText(objectMapper.writeValueAsString(
                        Result.error("消息发送失败：" + e.getMessage())
                ));
            } catch (IOException ex) {
                log.error("发送错误反馈失败", ex);
            }
        }
    }

    /**
     * 发送错误时的处理
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误,原因:" + error.getMessage());
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送消息
     */
    public void sendMessage(Object message) throws IOException {
        this.session.getBasicRemote().sendText(objectMapper.writeValueAsString(message));
    }

    // 广播消息
    public void sendAllMessage(String message) {
        log.info("【WebSocket】广播消息:" + message);
        for (WebSocket webSocket : webSockets) {
            try {
                if (webSocket.session.isOpen()) {
                    webSocket.session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 单点消息发送（优化版）
    public void sendOneMessage(Chat chat) throws Exception {
        Integer targetUserId = chat.getTargetId();
        Integer senderUserId = chat.getUserId();

        // 先保存消息到数据库
        boolean saveSuccess = chatService.saveChatMessage(chat);
        if (!saveSuccess) {
            throw new Exception("消息保存失败");
        }

        Session session = sessionPool.get(targetUserId);
        log.info("【WebSocket】单点消息: 发送者[{}] -> 接收者[{}], 内容:{}",
                senderUserId, targetUserId, chat.getContent());

        if (session != null && session.isOpen()) {
            try {
                // 发送完整的消息对象（包含发送者信息和时间）
                String messageJson = objectMapper.writeValueAsString(chat);
                // 正确实现 CompletionHandler 接口的两个抽象方法
                session.getAsyncRemote().sendText(messageJson, result -> {
                    if (result.isOK()) {
                        log.info("消息发送成功: 发送者[{}] -> 接收者[{}]", chat.getUserId(), chat.getTargetId());
                    } else {
                        log.error("消息发送失败: 发送者[{}] -> 接收者[{}]", chat.getUserId(), chat.getTargetId(), result.getException());
                    }
                });

            } catch (Exception e) {
                log.error("消息发送异常", e);
                throw e;
            }
        }
    }

    // 单点消息(多人)
    public void sendMoreMessage(String[] userIds, String message) {
        for (String userId : userIds) {
            Session session = sessionPool.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    log.info("【WebSocket】单点消息(多人):{}", message);
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
