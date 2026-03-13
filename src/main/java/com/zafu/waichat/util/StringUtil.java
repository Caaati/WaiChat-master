package com.zafu.waichat.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author yh
 * @create 2022-4-6 1:54
 */
public class StringUtil {

  public static String stringOf(Object o) {
    return o == null ? "" : o.toString();
  }

  public static String stringOf(Object o, String defaultString) {
    return o == null ? defaultString : o.toString();
  }

  public static boolean isNullOrEmpty(Object o) {
    return o == null || stringOf(o).isEmpty();
  }

  /**
   * 性别判断
   *
   * @param sex
   * @return
   */
  public static String isMaleOrFemale(String sex) {
    return "1".equals(sex) ? "男" : "2".equals(sex) ? "女" : "未知";
  }

  /**
   * 包装 oracle to_date 函数
   * 使用占位符
   * @description
   * @author yeh
   * @date 2022-5-11 11:02
   * @params [placeholder]
   * @return java.lang.String
   */
  public static String wrapDateConvert(String placeholder, String dateType) {
    if (placeholder.startsWith(":")) {
      return "TO_DATE(" + placeholder + ",'" + dateType + "')";
    } else {
      return "TO_DATE(:" + placeholder + ",'" + dateType + "')";
    }
  }

  public static String wrapDateConvert(String placeholder) {
    if (placeholder.startsWith(":")) {
      return "TO_DATE(" + placeholder + ",'yyyy-MM-dd')";
    } else {
      return "TO_DATE(:" + placeholder + ",'yyyy-MM-dd')";
    }
  }

  public static String wrapDateTimeConvert(String placeholder) {
    if (placeholder.startsWith(":")) {
      return "TO_DATE(" + placeholder + ",'yyyy-MM-dd HH24:mi:ss')";
    } else {
      return "TO_DATE(:" + placeholder + ",'yyyy-MM-dd HH24:mi:ss')";
    }
  }

  /**
   * 构造唯一的会话 Key
   */
  public static String getChatKey(String id1, String id2) {
    List<String> ids = Arrays.asList(id1, id2);
    Collections.sort(ids); // 排序，保证 A-B 和 B-A 生成同一个 Key
    return "chat:msg:" + ids.get(0) + ":" + ids.get(1);
  }
}
