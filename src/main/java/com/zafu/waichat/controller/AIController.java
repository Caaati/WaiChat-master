package com.zafu.waichat.controller;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zafu.waichat.mapper.LanguageMapper;
import com.zafu.waichat.pojo.dto.ChatDTO;
import com.zafu.waichat.pojo.dto.PolishDTO;
import com.zafu.waichat.pojo.dto.TranslateDTO;
import com.zafu.waichat.pojo.entity.Chat;
import com.zafu.waichat.pojo.entity.Language;
import com.zafu.waichat.pojo.vo.TranslateVO;
import com.zafu.waichat.util.MessageUtil;
import com.zafu.waichat.util.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
@Slf4j
@Api(tags = "AI相关接口")
public class AIController {
    @Autowired
    private LanguageMapper LanguageMapper;

    @GetMapping("/languages")
    public Result getLanguages() {
        try {
            List<Language> languages = LanguageMapper.selectList(new QueryWrapper<>());
            languages.forEach(l->{
//                l.setName(l.getChineseName());
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
                // 针对未指定风格的健壮性处理
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
     * @param chats 聊天历史记录列表
     * @return 回复文本
     */
    @PostMapping("/smartReply")
    public Result smartReply(@RequestBody List<ChatDTO> chats) {
        try {
            String sys = "\"你是一个高度专业且高效的多语言聊天回复模型。\n" +
                    "**角色:** 你代表历史聊天记录中的最后发言者 '我'。\n" +
                    "**目标:** 根据完整的聊天历史记录，尤其是**对方最近的一条消息**，生成一条最合适且自然的回复。\n" +
                    "**语言及语气要求:**\n" +
                    "1.  回复的**语言**必须与聊天历史中**对方最近一条消息**的语言保持一致。\n" +
                    "2.  回复的**语气和风格**必须模仿 '我' 在历史记录中展现的风格。\n" +
                    "3.  回复必须**简短、直接、实用**。\n" +
                    "**格式约束:** 严格要求你**只输出回复文本本身**，不允许包含任何多余的问候、解释、标点、或标签（如 '回复:'）。\"";
            StringBuilder historyText = new StringBuilder();
            chats.forEach(item -> {
                // 使用 userId 来区分发言者，并以 "发言者: 内容\n" 的形式拼装
                // 示例格式: 我: 2222\n对方: 6666666666\n
                String sender = item.getUserId();
                String content = item.getContent();
                if (sender != null && content != null) {
                    historyText.append(sender).append(": ").append(content).append("\n");
                }
            });
            // 添加回复指令或标识
            historyText.append("我: ");
            String userPrompt = historyText.toString();
            if (userPrompt.trim().isEmpty()) {
                return Result.error("聊天记录为空，无法生成摘要。");
            }
            GenerationResult back = MessageUtil.callWithMessageNormal(sys, userPrompt);
            String result = back.getOutput().getChoices().get(0).getMessage().getContent().trim();

            return Result.success(result);

        } catch (Exception e) {
            // 记录详细错误信息
            e.printStackTrace();
            return Result.error("智能回复生成失败: " + e.getMessage());
        }
    }

    /**
     * 聊天记录总结 API
     * @param chats 聊天历史记录列表
     * @return 总结后的文本
     */
    @PostMapping("/summarize")
    public Result summarize(@RequestBody List<ChatDTO> chats) {
        try {
            // 设置系统提示词，要求LLM以简洁、分点的形式总结
            String sys = "你是一个专业的聊天记录总结助手。" +
                    "你的任务是将用户提供的聊天历史记录，以简洁、清晰、分点(1,2,3)的形式总结出核心内容、关键决策、待办事项和未解决的问题。" +
                    "总结必须使用历史记录中的主要语言。只输出总结文本，不要包含任何额外说明或标题。";
            StringBuilder historyText = new StringBuilder();
            chats.forEach(item -> {
                // 拼装历史记录，格式与智能回复保持一致
                String sender = item.getUserId(); // 假设 userId 已经被映射为 "我" 或 "对方"
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
}
