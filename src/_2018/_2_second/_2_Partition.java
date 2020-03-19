package _2018._2_second;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class _2_Partition {
    public List<List<String>> partition(String s) {
        List<List<String>> result = new ArrayList<>();
        char[] aArray = s.toCharArray();
        for (int i = 0; i < aArray.length; i++) {
            for (int j = i; j < aArray.length; j++) {


            }
        }
        return result;
    }

    private boolean isPalindrome(String s, int begin, int last) {
        while(begin < last && s.charAt(begin) == s.charAt(last)){
            begin++;
            last--;
        }
        return begin >= last;
    }

    public int fourSumCount(int[] A, int[] B, int[] C, int[] D) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < B.length; j++) {
                map.put(A[i] + B[j], map.getOrDefault(A[i] + B[j], 0) + 1);
            }
        }
        int count = 0;
        for (int i = 0; i < C.length; i++) {
            for (int j = 0; j < D.length; j++) {
                count += map.getOrDefault(-C[i] - D[j], 0);
            }
        }
        return count;
    }
}
