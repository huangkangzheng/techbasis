package algorithm.leetcode;

/*
 * 字符串转换整数 (atoi)
 */
public class _8 {
    public int myAtoi(String str) {
        char[] array = str.toCharArray();
        for (int i = 0; i < array.length; i++) {
            if (isLegal(array[i])) {
                if (array[i] == '+') {
                    return calculate(i + 1, array, true);
                } else if (array[i] == '-') {
                    return calculate(i + 1, array, false);
                } else if (isNumber(array[i])) {
                    return calculate(i, array, true);
                }
            } else return 0;
        }
        return 0;
    }

    private boolean isLegal(char c) {
        return c == ' ' || c == '+' || c == '-' || isNumber(c);
    }

    private boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    private int calculate(int i, char[] array, boolean isPlus) {
        if (i > array.length - 1) {
            return 0;
        }
        int max = Integer.MAX_VALUE / 10;
        int min = Integer.MIN_VALUE / 10;
        int result = 0;
        if (isPlus) {
            while (i < array.length) {
                if (isNumber(array[i])) {
                    int tmp = array[i] - '0';
                    if (result > max || result == max && tmp > 7) return Integer.MAX_VALUE;
                    result = result * 10 + tmp;
                    i++;
                } else
                    break;
            }
        } else {
            while (i < array.length) {
                if (isNumber(array[i])) {
                    int tmp = array[i] - '0';
                    if (result < min || result == min && tmp > 8) return Integer.MIN_VALUE;
                    result = result * 10 - tmp;
                    i++;
                } else
                    break;
            }
        }
        return result;
    }
}
