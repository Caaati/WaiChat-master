package com.zafu.waichat.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LanguageEnum {
    ENGLISH("en", "English", "English", "英语"),
    CHINESE("zh", "中文", "Chinese", "中文"),
    TRADITIONAL_CHINESE("zh_tw", "繁體中文", "Traditional Chinese", "繁体中文"),
    RUSSIAN("ru", "Русский", "Russian", "俄语"),
    JAPANESE("ja", "日本語", "Japanese", "日语"),
    KOREAN("ko", "한국어", "Korean", "韩语"),
    SPANISH("es", "Español", "Spanish", "西班牙语"),
    FRENCH("fr", "Français", "French", "法语"),
    PORTUGUESE("pt", "Português", "Portuguese", "葡萄牙语"),
    GERMAN("de", "Deutsch", "German", "德语"),
    ITALIAN("it", "Italiano", "Italian", "意大利语"),
    THAI("th", "ไทย", "Thai", "泰语"),
    VIETNAMESE("vi", "Tiếng Việt", "Vietnamese", "越南语"),
    INDONESIAN("id", "Bahasa Indonesia", "Indonesian", "印度尼西亚语"),
    MALAY("ms", "Bahasa Melayu", "Malay", "马来语"),
    ARABIC("ar", "العربية", "Arabic", "阿拉伯语"),
    HINDI("hi", "हिन्दी", "Hindi", "印地语"),
    HEBREW("he", "עברית", "Hebrew", "希伯来语"),
    URDU("ur", "اردو", "Urdu", "乌尔都语"),
    BENGALI("bn", "বাংলা", "Bengali", "孟加拉语"),
    POLISH("pl", "Polski", "Polish", "波兰语"),
    DUTCH("nl", "Nederlands", "Dutch", "荷兰语"),
    TURKISH("tr", "Türkçe", "Turkish", "土耳其语"),
    KHMER("km", "ខ្មែរ", "Khmer", "高棉语"),
    CZECH("cs", "Čeština", "Czech", "捷克语"),
    SWEDISH("sv", "Svenska", "Swedish", "瑞典语"),
    HUNGARIAN("hu", "Magyar", "Hungarian", "匈牙利语"),
    DANISH("da", "Dansk", "Danish", "丹麦语"),
    FINNISH("fi", "Suomi", "Finnish", "芬兰语"),
    TAGALOG("tl", "Tagalog", "Tagalog", "他加禄语"),
    PERSIAN("fa", "فارسی", "Persian", "波斯语");


    private final String code;
    private final String displayName;
    private final String englishName;
    private final String chineseName;

    // 根据code获取LanguageEnum
    public static LanguageEnum getByCode(String code) {
        for (LanguageEnum lang : values()) {
            if (lang.code.equals(code)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("暂不支持的语言代码: " + code);
    }

    // 根据englishName获取LanguageEnum
    public static LanguageEnum getByEnglishName(String englishName) {
        // 判空，避免空指针
        if (englishName == null) {
            throw new IllegalArgumentException("语言英文名称不能为空");
        }
        // 遍历枚举，匹配englishName（忽略大小写，增强兼容性）
        for (LanguageEnum lang : values()) {
            if (lang.englishName.equalsIgnoreCase(englishName)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("暂不支持的语言英文名称: " + englishName);
    }
}
