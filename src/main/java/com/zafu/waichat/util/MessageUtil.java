package com.zafu.waichat.util;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.TranslationOptions;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {
    //    @Value("${spring.ai.dashscope.api-key}")
    private static String apiKey = "sk-b9bedc9945a2433fa4f6958d5b9a2552";

    public static String callWithMessageNormal(String sys, String user) throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(sys)
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(user)
                .build();
        GenerationParam param = GenerationParam.builder()
//                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .apiKey(apiKey)
                .model(ModelConstants.QW_FLASH)
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        GenerationResult result = gen.call(param);
        return result.getOutput().getChoices().get(0).getMessage().getContent().trim();
    }

//    public static String callWithMessageNormal(String sys, String user) throws ApiException, NoApiKeyException, UploadFileException {
//        MultiModalConversation conv = new MultiModalConversation();
//        List<Map<String, Object>> sysContentList = Arrays.asList(
//                Collections.singletonMap("text", sys)
//        );
//        List<Map<String, Object>> userContentList = Arrays.asList(
//                Collections.singletonMap("text", user)
//        );
//        MultiModalMessage systemMsg = MultiModalMessage.builder()
//                .role(Role.SYSTEM.getValue())
//                .content(sysContentList)
//                .build();
//        MultiModalMessage userMsg = MultiModalMessage.builder()
//                .role(Role.USER.getValue())
//                .content(userContentList)
//                .build();
//        MultiModalConversationParam  param = MultiModalConversationParam.builder()
//                .apiKey(apiKey)
//                .model(ModelConstants.QW_FLASH)
//                .messages(Arrays.asList(systemMsg, userMsg))
//                .build();
//        MultiModalConversationResult result = conv.call(param);
//        return stringOf(result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));
//    }

    public static GenerationResult translateWithTarget(String msg, String target) throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(msg)
                .build();
        TranslationOptions options = TranslationOptions.builder()
                .sourceLang("auto")
                .targetLang(target)
                .build();
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(ModelConstants.MT_LITE)
                .messages(Collections.singletonList(userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .translationOptions(options)
                .build();
        return gen.call(param);
    }

    public static GenerationResult translateWithModel(String msg, String target, String model) throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(msg)
                .build();
        TranslationOptions options = TranslationOptions.builder()
                .sourceLang("auto")
                .targetLang(target)
                .build();
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(Collections.singletonList(userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .translationOptions(options)
                .build();
        return gen.call(param);
    }

    public static MultiModalConversationResult audioCaptioner(File file) throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        // 使用本地文件的绝对路径进行调用
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("audio", file.getAbsolutePath())))
                .build();
        MultiModalMessage sysMessage = MultiModalMessage.builder().role(Role.SYSTEM.getValue())
                // 此处用于配置定制化识别的Context
                .content(Arrays.asList(Collections.singletonMap("text", "")))
                .build();

        Map<String, Object> asrOptions = new HashMap<>();
        asrOptions.put("enable_itn", false);
        // asrOptions.put("language", "zh"); // 可选，若已知音频的语种，可通过该参数指定待识别语种，以提升识别准确率
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(ModelConstants.QW_AUDIO)
                .message(sysMessage)
                .message(userMessage)
                .parameter("asr_options", asrOptions)
                .build();
        return conv.call(param);
    }

    public static String voiceToText(String audioUrl) throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("audio", audioUrl)))
                .build();

        // 修复：必须为 sysMessage 设置 content，不能为空
        MultiModalMessage sysMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Arrays.asList(Collections.singletonMap("text", "")))
                .build();

        Map<String, Object> asrOptions = new HashMap<>();
        asrOptions.put("enable_itn", false);
        // asrOptions.put("language", "zh"); // 可选，若已知音频的语种，可通过该参数指定待识别语种，以提升识别准确率
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 新加坡/美国地域和北京地域的API Key不同。获取API Key：https://help.aliyun.com/zh/model-studio/get-api-key
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(apiKey)
                // 若使用美国地域的模型，需在模型后面加上"-us"后缀，例如 qwen3-asr-flash-us
                .model("qwen3-asr-flash")
                .message(sysMessage)
                .message(userMessage)
                .parameter("asr_options", asrOptions)
                .build();
        MultiModalConversationResult result = conv.call(param);
        return JsonUtils.toJson(result);
    }
}
