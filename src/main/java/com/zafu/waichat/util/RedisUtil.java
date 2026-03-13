package com.zafu.waichat.util;

import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类（封装 String、List、Hash、Set、ZSet 等常用操作）
 */
@Component
public class RedisUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    // ====================== String 类型操作======================
    /**
     * 设置字符串值（无过期时间）
     */
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 获取字符串值
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 设置字符串值并指定过期时间
     * @param timeout 过期时间（单位：传入的 TimeUnit）
     */
    public void setWithExpire(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 重载：设置过期时间（默认秒）
     */
    public void setWithExpire(String key, String value, long timeout) {
        setWithExpire(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 原子递增（值为数字时使用）
     * @param delta 递增步长（正数增，负数减）
     */
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 判断键是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 删除单个键
     */
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 批量删除键
     */
    public void deleteBatch(Collection<String> keys) {
        stringRedisTemplate.delete(keys);
    }

    /**
     * 设置键的过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(stringRedisTemplate.expire(key, timeout, unit));
    }

    /**
     * 获取键的剩余过期时间（单位：秒）
     * @return -1：永久有效，-2：键不存在，其他：剩余秒数
     */
    public Long getExpire(String key) {
        return stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // ====================== List 类型操作 ======================
    /**
     * 向 List 尾部添加元素
     */
    public Long lPush(String key, String... values) {
        return stringRedisTemplate.opsForList().rightPushAll(key, values);
    }

    /**
     * 向 List 头部添加元素
     */
    public Long rPush(String key, String... values) {
        return stringRedisTemplate.opsForList().leftPushAll(key, values);
    }

    /**
     * 获取 List 中指定范围的元素
     * @param start 起始索引（0 开始）
     * @param end 结束索引（-1 表示最后一个）
     */
    public List<String> lRange(String key, long start, long end) {
        return stringRedisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取 List 长度
     */
    public Long lSize(String key) {
        return stringRedisTemplate.opsForList().size(key);
    }

    /**
     * 获取 List 指定索引的元素
     */
    public String lIndex(String key, long index) {
        return stringRedisTemplate.opsForList().index(key, index);
    }

    /**
     * 删除 List 中指定值的元素
     * @param count 计数：0-删除所有，正数-从头部删count个，负数-从尾部删count个
     */
    public Long lRemove(String key, long count, String value) {
        return stringRedisTemplate.opsForList().remove(key, count, value);
    }

    /**
     * 修剪 List，保留指定区间内的元素，其他的删除
     * 用于控制热数据量
     */
    public void lTrim(String key, long start, long end) {
        stringRedisTemplate.opsForList().trim(key, start, end);
    }

    /**
     * 移除并获取列表最后一个元素（可用于把旧热数据移动到别处或确认丢弃）
     */
    public String rPop(String key) {
        return stringRedisTemplate.opsForList().rightPop(key);
    }

    // ====================== Hash 类型操作 ======================
    /**
     * 向 Hash 中添加单个字段
     */
    public void hPut(String key, String hashKey, String value) {
        stringRedisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 向 Hash 中批量添加字段
     */
    public void hPutAll(String key, Map<String, String> hashMap) {
        stringRedisTemplate.opsForHash().putAll(key, hashMap);
    }

    /**
     * 获取 Hash 中指定字段的值
     */
    public String hGet(String key, String hashKey) {
        return (String) stringRedisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取 Hash 中所有字段和值
     */
    public Map<String, String> hGetAll(String key) {
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(key);
        Map<String, String> resultMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            resultMap.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return resultMap;
    }

    /**
     * 获取 Hash 中所有字段名
     */
    public Set<String> hKeys(String key) {
        Set<Object> keys = stringRedisTemplate.opsForHash().keys(key);
        return new HashSet<>(keys.stream().map(Object::toString).toList());
    }

    /**
     * 获取 Hash 中所有值
     */
    public List<String> hValues(String key) {
        List<Object> values = stringRedisTemplate.opsForHash().values(key);
        return values.stream().map(Object::toString).toList();
    }

    /**
     * 判断 Hash 中是否存在指定字段
     */
    public boolean hHasKey(String key, String hashKey) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForHash().hasKey(key, hashKey));
    }

    /**
     * 删除 Hash 中指定字段
     */
    public Long hDelete(String key, String... hashKeys) {
        return stringRedisTemplate.opsForHash().delete(key, (Object[]) hashKeys);
    }

    // ====================== Set 类型操作 ======================
    /**
     * 向 Set 中添加元素
     */
    public Long sAdd(String key, String... values) {
        return stringRedisTemplate.opsForSet().add(key, values);
    }

    /**
     * 获取 Set 中所有元素
     */
    public Set<String> sMembers(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    /**
     * 判断 Set 中是否包含指定元素
     */
    public boolean sIsMember(String key, String value) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, value));
    }

    /**
     * 获取 Set 大小
     */
    public Long sSize(String key) {
        return stringRedisTemplate.opsForSet().size(key);
    }

    /**
     * 从 Set 中删除指定元素
     */
    public Long sRemove(String key, String... values) {
        return stringRedisTemplate.opsForSet().remove(key, (Object[]) values);
    }

    /**
     * 随机获取 Set 中的一个元素
     */
    public String sRandomMember(String key) {
        return stringRedisTemplate.opsForSet().randomMember(key);
    }

    // ====================== ZSet（有序集合）操作 ======================
    /**
     * 向 ZSet 中添加元素（带分数）
     */
    public Boolean zAdd(String key, String value, double score) {
        return stringRedisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 获取 ZSet 中指定元素的分数
     */
    public Double zScore(String key, String value) {
        return stringRedisTemplate.opsForZSet().score(key, value);
    }

    /**
     * 按分数升序获取 ZSet 中指定范围的元素
     * @param start 起始排名（0 开始）
     * @param end 结束排名（-1 表示最后一个）
     */
    public Set<String> zRange(String key, long start, long end) {
        return stringRedisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 按分数降序获取 ZSet 中指定范围的元素
     */
    public Set<String> zReverseRange(String key, long start, long end) {
        return stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取 ZSet 大小
     */
    public Long zSize(String key) {
        return stringRedisTemplate.opsForZSet().size(key);
    }

    /**
     * 删除 ZSet 中指定元素
     */
    public Long zRemove(String key, String... values) {
        return stringRedisTemplate.opsForZSet().remove(key, (Object[]) values);
    }

    // ====================== 分布式锁（基础版） ======================
    /**
     * 获取分布式锁
     * @param lockKey 锁键
     * @param requestId 请求ID（用于标识锁的归属，避免误删）
     * @param expireTime 锁过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(lockKey, requestId, expireTime, TimeUnit.SECONDS));
    }

    /**
     * 释放分布式锁
     * @param lockKey 锁键
     * @param requestId 请求ID（必须与加锁时一致）
     */
    public boolean releaseLock(String lockKey, String requestId) {
        // 使用 Lua 脚本保证原子性（避免并发下误删其他线程的锁）
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(lockKey),
                requestId
        );
        return result != null && result > 0;
    }
}