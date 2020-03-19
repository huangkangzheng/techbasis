package _2018._2_second;

/*
 * 2个指针left和right，left从左向右走，right从右向左走
 * 满足条件的a[left]和a[right]必须要相等
 * 循环结束条件是left >= right
 */
public class _1_IsPalindrome {
    public boolean isPalindrome(String s) {
        char[] aArray = s.toLowerCase().toCharArray();
        if (aArray.length == 0)
            return true;
        int left = 0;
        int right = aArray.length - 1;
        while (left < right) {
            if (!valid(aArray[left])) {
                left++;
                continue;
            }
            if (!valid(aArray[right])) {
                right--;
                continue;
            }
            if (aArray[left] == aArray[right]) {
                left++;
                right--;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean valid(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' ||
                c >= '0' && c <= '9';
    }
}
