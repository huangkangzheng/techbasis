package leetcode;

import java.util.HashMap;
import java.util.Map;

/*
 * 最长回文串
 */
public class _409 {
    public int longestPalindrome(String s) {
        char[] array = s.toCharArray();
        Map<Character, Integer> map = new HashMap<>();
        for (char c : array) {
            map.put(c, map.get(c) == null ? 1 : map.get(c) + 1);
        }
        int result = 0;
        int isOdd = 0;
        for (Map.Entry<Character, Integer> entry : map.entrySet()) {
            if (entry.getValue() == 1) {
                isOdd = 1;
            }
            if (entry.getValue() > 1) {
                if (entry.getValue() % 2 == 0) {
                    result += entry.getValue();
                } else {
                    result += entry.getValue() - 1;
                    isOdd = 1;
                }
            }
        }
        return result + isOdd;
    }
}
