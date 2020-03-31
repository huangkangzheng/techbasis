package algorithm.leetcode;

import java.util.HashMap;
import java.util.Map;

/*
 * 最长回文串
 * 贪心算法，所有出现偶数次的字母都是最长回文串的一部分
 * 若是出现大于2的奇数次x，则x - 1个该字母可作为最长回文串的一部分，且多余的1个可作为中心
 * 若有出现奇数次的字母，则该字母可以作为最长回文串的中心
 * 故而最长回文串的长度 = 出现偶数次字母数量 + 是否有奇数次的字母
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
