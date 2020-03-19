package leetcode;

/*
 * Z型变化
 * Z的两列之间的字符的下标，间距为gap = 2 * numRows - 2
 * 第一行的index为 it * gap
 * 中间行数，每次迭代都会有2个字符，index分别为it * gap - row 和it * gap + row
 * 最后一行的index为 it * gap + numRows - 1
 */
public class _6 {
    public String convert(String s, int numRows) {
        if (numRows == 1) {
            return s;
        }
        int len = s.length();
        StringBuilder sb = new StringBuilder();
        int gap = 2 * numRows - 2;
        // first row
        int it = 0;
        while (it * gap < len) {
            sb.append(s.charAt(it * gap));
            it++;
        }
        for (int row = 1; row < numRows - 1; row++) {
            it = 0;
            while (it * gap - row < len) {
                if (it > 0) {
                    sb.append(s.charAt(it * gap - row));
                }
                if (it * gap + row < len) {
                    sb.append(s.charAt(it * gap + row));
                }
                it++;
            }
        }
        //last row
        it = 0;
        while (it * gap + numRows - 1 < len) {
            sb.append(s.charAt(it * gap + numRows - 1));
            it++;
        }
        return sb.toString();
    }
}
