package com.yupi.yuaicodemother.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2025/10/5
 * @description
 */
public class ThreadLocalUtil {
    private final static ThreadLocal<Map<String, String>> THREAD_LOCAL_USER_ID = new ThreadLocal<>();

    public static void set(String key, String value) {
         if (THREAD_LOCAL_USER_ID.get() == null) {
             HashMap<String, String> inertValue = new HashMap<>();
             inertValue.put(key, value);
             THREAD_LOCAL_USER_ID.set(inertValue);
         } else {
             THREAD_LOCAL_USER_ID.get().put(key, value);
         }
    }

    public static String getAndRemove(String key) {
        String res = THREAD_LOCAL_USER_ID.get().get(key);
        THREAD_LOCAL_USER_ID.remove();
        return res;
    }

    public static String get(String key) {
        return THREAD_LOCAL_USER_ID.get().get(key);
    }

    public static void remove() {
        THREAD_LOCAL_USER_ID.remove();
    }
}
