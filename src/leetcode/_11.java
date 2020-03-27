package leetcode;

/*
 * 盛最多水的容器
 * 对于横坐标i, j, 最大的面积等于min(height[i], height[j]) * (j - i)
 * 暴力法，复杂度为O(n 2)
 */
public class _11 {
    public int maxArea(int[] height) {
        int max = 0;
        for (int i = 0; i < height.length; i++) {
            for (int j = i + 1; j < height.length; j++) {
                int area = Math.min(height[i], height[j]) * (j - i);
                if (area > max) max = area;
            }
        }
        return max;
    }

/*
 * 双指针法，最初i为0，j为height.length - 1，此时宽度为最大宽度，得到最大面积max
 * 若是想增大最大面积max，则需要移动较短侧的指针
 * 因为移动指针会导致宽度变短，但是可能得到更高的高度，从而得到更大的面积
 * 终止条件为i >= j
 */
    public int maxArea2(int[] height) {
        int max = 0;
        int i = 0; int j = height.length - 1;
        while (i < j) {
            max = Math.max(Math.min(height[i], height[j]) * (j - i), max);
            if (height[i] < height[j]) {
                i++;
            } else {
                j--;
            }
        }
        return max;
    }
}
