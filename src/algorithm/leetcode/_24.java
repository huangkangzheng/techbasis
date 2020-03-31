package algorithm.leetcode;

/*
 * 两两交换链表中的节点
 */
public class _24 {
    public ListNode swapPairs(ListNode head) {
        if (head == null || head.next == null)
            return head;
        // 链表长度大于1，结果链表头一定为head.next
        ListNode result = head.next;
        ListNode p1 = head;
        // 每次交换2个节点，用于保存偶数节点，为了链上下一次交换后的值
        // 比如1 -> 2 -> 3 -> 4
        // 交换为2 -> 1 -> 3 -> 4
        // 则dummy为1
        //下次交换使4 -> 3，但是需要使1 -> 4
        // 故而保存dummy节点
        ListNode dump = null;
        while (p1.next != null) {
            ListNode p2 = p1.next;
            ListNode p3 = p2.next;
            p2.next = p1;
            p1.next = p3;
            if (dump != null) {
                dump.next = p2;
            }
            dump = p1;
            p1 = p3;
            if (p1 == null) break;
        }
        return result;
    }
}
