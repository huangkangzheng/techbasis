package leetcode;

/*
 * 两个有序数组的中位数
 */
public class _4 {
    // 未解出来
    public double findMedianSortedArrays(int[] nums1, int[] nums2) {
        int m = nums1.length;
        int n = nums2.length;
        boolean isOdd = (m + n) % 2 != 0;
        int median = isOdd ? (m + n) / 2 + 1 : (m + n) / 2;
        if (m == 0) {
            return isOdd ? nums2[median - 1] : (nums2[median - 1] + nums2[median]) / 2.0;
        }
        if (n == 0) {
            return isOdd ? nums1[median - 1] : (nums1[median - 1] + nums1[median]) / 2.0;
        }
        int count = 0;
        int i = 0;
        int j = 0;
        int tmp = 0;
        while (count < median) {
            if (nums1[i] < nums2[j]) {
                if (i != m - 1) {
                    tmp = nums1[i != m - 1 ? i++ : m - 1];
                    count++;
                }
            } else {
                tmp = nums2[j != n - 1 ? j++ : n - 1];
                count++;
            }
        }
        if (isOdd) {
            return tmp;
        } else {
            return tmp == nums1[i == m - 1 ? i : i - 1] ? (tmp + nums2[j]) / 2.0 : (tmp + nums1[i == m - 1 ? i : i - 1]) / 2.0;
        }
    }
}
