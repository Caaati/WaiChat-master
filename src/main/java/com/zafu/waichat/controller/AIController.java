package com.zafu.waichat.controller;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zafu.waichat.mapper.LanguageMapper;
import com.zafu.waichat.mapper.UserMapper;
import com.zafu.waichat.pojo.dto.ChatDTO;
import com.zafu.waichat.pojo.dto.PolishDTO;
import com.zafu.waichat.pojo.dto.TranslateDTO;
import com.zafu.waichat.pojo.entity.Language;
import com.zafu.waichat.pojo.entity.User;
import com.zafu.waichat.pojo.vo.TranslateVO;
import com.zafu.waichat.util.MessageUtil;
import com.zafu.waichat.util.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@Slf4j
@Api(tags = "AI相关接口")
public class AIController {
    @Autowired
    private LanguageMapper LanguageMapper;
    @Autowired
    private UserMapper userMapper;

    @GetMapping("/languages")
    public Result getLanguages() {
        try {
            List<Language> languages = LanguageMapper.selectList(new QueryWrapper<>());
            languages.forEach(l -> {
//                l.setName(l.getChineseName());
                // 使用各语言的语言名称
                l.setName(l.getDisplayName());
            });
            return Result.success(languages);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/translate")
    public Result translate(@RequestBody TranslateDTO translateDTO) {
        try {
            GenerationResult back = MessageUtil.translateWithTarget(translateDTO.getText(), translateDTO.getTarget());
            String result = back.getOutput().getChoices().get(0).getMessage().getContent();
            TranslateVO vo = new TranslateVO();
            vo.setTranslated(result);
            vo.setOriginal(translateDTO.getText());
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/polish")
    public Result polish(@RequestBody PolishDTO polishDTO) {
        try {
            String text = polishDTO.getText();
            String style = polishDTO.getStyle();
            String sys = "你是一个专业的多功能文本润色和风格调整AI。你的任务是严格按照用户指定的风格，对输入的文本进行优化。";
            if (style.equals("business")) {
                sys += "【风格要求】将文本润色为**正式、专业、严谨的商务风格**。使用礼貌且清晰的措辞，确保信息传达精确无误。";
            } else if (style.equals("casual")) {
                sys += "【风格要求】将文本调整为**友好、轻松、非正式的休闲风格**（语气软化）。消除任何可能存在的攻击性或生硬感，使其听起来更自然亲切。";
            } else {
                sys += "【风格要求】未指定风格，请进行基础的语法和表达优化。";
            }
            sys += "【语言约束】润色后的文本**必须保持与原文本相同的语言**。" +
                    "【格式约束】严格要求**只输出润色或调整后的文本本身**，不允许包含任何额外的解释、说明、标签或标点。";
            GenerationResult back = MessageUtil.callWithMessageNormal(sys, text);
            String result = back.getOutput().getChoices().get(0).getMessage().getContent().trim();
            // 增加对空内容的校验，防止模型返回空字符串
            if (result.isEmpty()) {
                return Result.error("AI未能生成润色结果，请尝试调整原文。");
            }
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("润色服务异常: " + e.getMessage());
        }
    }

    /**
     * 智能回复 API
     *
     * @param chats 聊天历史记录列表
     * @return 回复文本
     */
    @PostMapping("/smartReply")
    public Result smartReply(@RequestBody List<ChatDTO> chats) {
        try {
            String sys1 = "\"你是一个高度专业且高效的多语言聊天回复模型。\n";
            String sys_role = "**角色:** 你代表历史聊天记录中的发言者 '我'。\n";
            ChatDTO chatIds = chats.get(chats.size() - 1);
            // 处理用户ID和目标用户信息
            if (!"我".equals(chatIds.getUserId()) && !"对方".equals(chatIds.getUserId())) {
                chats.remove(chatIds);
                Integer userId = Integer.valueOf(chatIds.getUserId());
                Integer targetId = Integer.valueOf(chatIds.getTargetId());
                User user = userMapper.selectById(userId);
                User target = userMapper.selectById(targetId);
                sys_role += "**'我'的用户信息:** **昵称:**" + user.getNickname() + "**用户名:**" + user.getUsername() + "**ID:**" + user.getId() + "\n" +
                        "**'对方'的用户信息:** **昵称:**" + target.getNickname() + "**用户名:**" + target.getUsername() + "**ID:**" + target.getId() + "\n";
            }
            // 判断聊天记录的最后一条有效消息的发言者（排除末尾的ID对象）
            ChatDTO lastValidChat = null;
            for (int i = chats.size() - 1; i >= 0; i--) {
                ChatDTO chat = chats.get(i);
                // 有效消息需包含userId（我/对方）和content
                if (("我".equals(chat.getUserId()) || "对方".equals(chat.getUserId())) && chat.getContent() != null && !chat.getContent().trim().isEmpty()) {
                    lastValidChat = chat;
                    break;
                }
            }
            // 若无有效聊天记录，直接返回错误
            if (lastValidChat == null) {
                return Result.error("无有效聊天记录，无法生成智能回复。");
            }
            // 动态调整系统指令
            String finalSpeaker = lastValidChat.getUserId();
            String sys2 = "**目标:** 根据完整的聊天历史记录，尤其是**对方最近的一条消息**，生成一条最合适且自然的回复。\n" +
                    "**语言及语气要求:**\n" +
                    "1.  回复的**语言**必须与聊天历史中** '我' 最近一条消息**的语言保持一致。\n" +
                    "2.  回复的**语气和风格**必须模仿 '我' 在历史记录中展现的风格。\n" +
                    "3.  回复必须**简短、直接、实用**。\n" +
                    "**格式约束:** 严格要求你**只输出回复文本本身**，不允许包含任何多余的问候、解释、标点、或标签（如 '回复:'）。\"";
            if ("对方".equals(finalSpeaker)) {
                // 最后发言者是对方：生成我对对方的回复
                sys2 += "**注意:** 以'我'的视角回复对方的最后一条消息。";
            } else {
                // 最后发言者是我：生成我的下一句话，延续自己的发言逻辑
                sys2 += "**注意:** 以'我'的视角继续发言，生成我的下一句话，无需回复对方（当前无对方新消息）。";
            }

            // 拼接历史消息（原有逻辑保留，确保格式正确）
            StringBuilder historyText = new StringBuilder();
            chats.forEach(item -> {
                String sender = item.getUserId();
                String content = item.getContent();
                if (sender != null && content != null && !content.trim().isEmpty()) {
                    historyText.append(sender).append(": ").append(content).append("\n");
                }
            });
            historyText.append("我: ");
            String userPrompt = historyText.toString();
            if (userPrompt.trim().isEmpty()) {
                return Result.error("聊天记录为空，无法生成摘要。");
            }
            // 调用大模型生成回复
            GenerationResult back = MessageUtil.callWithMessageNormal(sys1 + sys_role + sys2, userPrompt);
            String result = back.getOutput().getChoices().get(0).getMessage().getContent().trim();
            return Result.success(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("智能回复生成失败: " + e.getMessage());
        }
    }

    /**
     * 聊天记录总结 API
     *
     * @param chats 聊天历史记录列表
     * @return 总结后的文本
     */
    @PostMapping("/summarize")
    public Result summarize(@RequestBody List<ChatDTO> chats) {
        try {
            String sys = "你是一个专业的聊天记录总结助手。" +
                    "你的任务是将用户提供的聊天历史记录，以简洁、清晰、分点(1,2,3)的形式总结出核心内容、关键决策、待办事项和未解决的问题。" +
                    "总结必须使用历史记录中的主要语言。只输出总结文本，不要包含任何额外说明或标题。";
            StringBuilder historyText = new StringBuilder();
            chats.forEach(item -> {
                String sender = item.getUserId();
                String content = item.getContent();
                if (sender != null && content != null) {
                    historyText.append(sender).append(": ").append(content).append("\n");
                }
            });
            String userPrompt = historyText.toString();
            if (userPrompt.trim().isEmpty()) {
                return Result.error("聊天记录为空，无法生成摘要。");
            }
            GenerationResult back = MessageUtil.callWithMessageNormal(sys, userPrompt);
            String result = back.getOutput().getChoices().get(0).getMessage().getContent().trim();
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("聊天摘要生成失败: " + e.getMessage());
        }
    }

    /**
     * 聊天数据分析 API (Dashboard)
     * 后端负责解析 AI 返回的 JSON 字符串
     */
    @PostMapping("/analysis")
    public Result analysis(@RequestBody List<java.util.Map<String, String>> chats) {
        try {
            // 准备系统提示词
            String sys = "你是一个专业的数据分析师。请分析用户提供的聊天记录，并输出一个严格的 JSON 对象（不要包含Markdown代码块符号）。" +
                    "JSON 结构必须包含以下字段：" +
                    "1. 'keywords': 一个数组，包含前 8 个高频关键词，格式为 [{'name': '词语', 'value': 频率数值}]。" +
                    "2. 'sentiment': 一个数组，包含最后 10 条消息的情感评分（1代表积极，0代表中性，-1代表消极）。" +
                    "3. 'summary': 一段简短有趣的关于这段关系的总结（50字以内）。" +
                    "请忽略常见的停用词（如'的', '了', '是'等）。";
            // 拼装历史记录
            StringBuilder historyText = new StringBuilder();
            int start = Math.max(0, chats.size() - 50);
            for (int i = start; i < chats.size(); i++) {
                java.util.Map<String, String> item = chats.get(i);
                String sender = item.get("userId");
                String content = item.get("content");
                if (sender != null && content != null) {
                    historyText.append(sender).append(": ").append(content).append("\n");
                }
            }
            String userPrompt = historyText.toString();
            if (userPrompt.trim().isEmpty()) {
                return Result.error("记录为空");
            }

            GenerationResult back = MessageUtil.callWithMessageNormal(sys, userPrompt);
            String resultJson = back.getOutput().getChoices().get(0).getMessage().getContent().trim();

            // 清理 Markdown 符号
            if (resultJson.startsWith("```")) {
                resultJson = resultJson.replaceAll("^```json", "").replaceAll("^```", "").replaceAll("```$", "");
            }
            // 使用 Jackson ObjectMapper 在后端解析 JSON
            ObjectMapper mapper = new ObjectMapper();
            // 解析为 Map<String, Object> 结构，方便 Spring Boot 自动序列化
            Map<String, Object> analysisMap = mapper.readValue(resultJson, new TypeReference<Map<String, Object>>() {
            });

            return Result.success(analysisMap);

        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            // 捕获 JSON 解析错误，说明 LLM 返回了不规范的格式
            e.printStackTrace();
            return Result.error("AI返回数据格式不规范，请重试或检查模型输出。");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("分析服务失败: " + e.getMessage());
        }
    }

    @PostMapping("/audio/stt")
    public Result transcribe(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        File tempFile = null;
        try {
            // 将 MultipartFile 保存为临时文件
            // 获取系统临时目录并创建一个前缀为 "stt_" 的临时文件
            tempFile = File.createTempFile("stt_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);
            MultiModalConversationResult result = MessageUtil.audioCaptioner(tempFile);
            // 获取返回的第一个回答的第一个内容块中的文本信息
            JsonObject jsonObject = JsonUtils.toJsonObject(result);
            String message = String.valueOf(jsonObject.get("message"));
            return Result.success(message);
        } catch (NoApiKeyException e) {
            log.error("API Key 缺失: {}", e.getMessage());
            return Result.error("服务器 AI 配置异常");
        } catch (UploadFileException e) {
            log.error("文件上传到 DashScope 失败: {}", e.getMessage());
            return Result.error("语音解析失败：上传异常");
        } catch (ApiException e) {
            log.error("DashScope API 调用异常: {}", e.getMessage());
            return Result.error("AI 服务暂不可用");
        } catch (IOException e) {
            log.error("文件处理异常: {}", e.getMessage());
            return Result.error("系统文件错误");
        } finally {
            // 清理临时文件，防止磁盘写满
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
