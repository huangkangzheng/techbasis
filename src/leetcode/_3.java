package leetcode;

import java.util.HashSet;
import java.util.Set;

/*
 * 最长无重复子串问题（子串意味着连续）
 * 无重复选用HashSet
 * HashSet作为滑动窗口，i是left，j是right
 * 窗口前进：i不变j递增，若是无重复则放入set，记录max值
 * 遇到重复则递增i，同时remove掉左边的值，直到遇到重复的值，窗口可以继续前进
 */

public class _3 {
    public int lengthOfLongestSubstring(String s) {
        char[] array = s.toCharArray();
        Set<Character> set = new HashSet<>();
        int max = 0;
        int i = 0;
        int j = 0;
        while (i < array.length && j < array.length) {
            if (!set.contains(array[j])) {
                set.add(array[j++]);
                max = Math.max(max, j + 1 - i);
            } else {
                set.remove(array[i++]);
            }
        }
        return max;
    }
}
