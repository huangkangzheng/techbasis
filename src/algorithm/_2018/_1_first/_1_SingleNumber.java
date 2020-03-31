package algorithm._2018._1_first;

/*
 * 0与同一个数异或两次仍然为0
 */
class _1_SingleNumber {
    public int singleNumber(int[] nums) {
        int result = 0;
        for (int i : nums) {
            result ^= i;
        }
        return result;
    }
}
