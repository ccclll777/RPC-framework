package org.rpc.utils;

/**
 * String 工具类
 *
 */
public class StringUtil {

    public static boolean isBlank(String s) {
        /*
        是否为空字符串
         */
        if (s == null || s.length() == 0) {
            return true;
        }
        for (int i = 0; i < s.length(); ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
