package algorithm._2018._1_first;

/*
 * 摩尔投票算法
 */
public class _2_MajorityElement {
    public int majorityElement(int[] nums) {
        int major = nums[0];
        int count = 0;
        for (int i = 0; i < nums.length; i++) {
            if (count == 0) {
                major = nums[i];
                count = 0;
            }
            if (major == nums[i]) {
                count++;
            } else {
                count--;
            }
        }
        return major;
    }
}
