package leetcode;

/*
 * 整数反转（给定x∈[−2 31,  2 31 − 1]），进行反转
 * 取值范围为 -2147483648——2147483647
 * 采用数学方法进行变换
 * 溢出的边界情况为：做最后一步操作前
 * 正数：如果当前y > max / 10 || y == max / 10 && 个位数i > 7
 * 负数：如果当前y < min / 10 || y == min / 10 && 个位数i < -8
 */
public class _7 {
    public int reverse(int x) {
        int y = 0;
        int max = Integer.MAX_VALUE / 10;
        int min = Integer.MIN_VALUE / 10;
        while (x != 0) {
            int i = x % 10;
            x = x / 10;
            if (y > max || (y == max && i > 7)) return 0;
            if (y < min || (y == min && i < -8)) return 0;
            y = y * 10 + i;
        }
        return y;
    }
}
