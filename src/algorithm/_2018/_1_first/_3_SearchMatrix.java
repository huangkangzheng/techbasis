package algorithm._2018._1_first;

/*
 * 从右上角到右下角遍历，大的往下走，小的往左走
 */
public class _3_SearchMatrix {
    public boolean searchMatrix(int[][] matrix, int target) {
        if (matrix.length == 0 || matrix[0].length == 0) return false;
        int i = 0;
        int j = matrix[0].length - 1;
        while (j >= 0 && i < matrix.length) {
            if (target == matrix[i][j])
                return true;
            else if (target > matrix[i][j]) {
                i++;
            } else {
                j--;
            }
        }
        return false;
    }
}
