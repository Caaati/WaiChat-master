// src/main/java/com/zafu/waichat/service/impl/ChatServiceImpl.java
package com.zafu.waichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zafu.waichat.mapper.ChatMapper;
import com.zafu.waichat.pojo.dto.UserContactDTO;
import com.zafu.waichat.pojo.entity.Chat;
import com.zafu.waichat.service.ChatService;
import com.zafu.waichat.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.zafu.waichat.util.StringUtil.getChatKey;

@Service
@Slf4j
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements ChatService {

    @Autowired
    private ChatMapper chatMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public List<Chat> getChatHistory(Long userId1, Long userId2) {
        String redisKey = getChatKey(String.valueOf(userId1), String.valueOf(userId2));

        // 1. 尝试从 Redis 获取热数据 (最近 50 条)
        List<String> rawMsgs = redisUtil.lRange(redisKey, 0, -1);

        if (rawMsgs != null && !rawMsgs.isEmpty()) {
            log.info("从 Redis 加载热数据");
            return rawMsgs.stream()
                    .map(s -> {
                        try { return objectMapper.readValue(s, Chat.class); }
                        catch (Exception e) { return null; }
                    })
                    .sorted(Comparator.comparing(Chat::getCreateTime)) // 内存排序
                    .toList();
        }

        // 2. Redis 没有，查询数据库（冷数据）
        log.info("Redis 缺失，从 MySQL 加载数据并回填");
        List<Chat> dbHistory = chatMapper.selectChatHistory(userId1, userId2);

        // 3. 回填热数据到 Redis (只回填最近的 50 条)
        if (!dbHistory.isEmpty()) {
            List<Chat> hotPart = dbHistory.size() > 50 ?
                    dbHistory.subList(dbHistory.size() - 50, dbHistory.size()) : dbHistory;
            hotPart.forEach(c -> {
                try {
                    redisUtil.lPush(redisKey, objectMapper.writeValueAsString(c));
                } catch (Exception ignored) {}
            });
            redisUtil.expire(redisKey, 7, TimeUnit.DAYS);
        }

        return dbHistory;
    }

    @Override
    public boolean saveChatMessage(Chat chat) {
        return this.save(chat);
    }

    @Override
    public List<UserContactDTO> getContactList(Long userId) {
        return chatMapper.selectContactList(userId);
    }

    @Override
    public void removeHistory(Integer userId, Integer targetId) {
        int update = chatMapper.updateStatus(userId, targetId,0);
        log.info("成功更新{}行数据", update);
    }

    @Override
    public void recoverHistory(Integer userId, Integer targetId) {
        int update = chatMapper.updateStatus(userId, targetId,1);
        log.info("成功更新{}行数据", update);
    }
}
