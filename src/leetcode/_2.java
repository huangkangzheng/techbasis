package leetcode;

/*
 * 两个链表相加
 * 数学加法
 */
public class _2 {
    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode p1 = l1;
        ListNode p2 = l2;
        int carry = 0;
        ListNode dummyHead = new ListNode(0);
        ListNode p3 = dummyHead;
        while (p1 != null || p2 != null) {
            int v1 = p1 != null ? p1.val : 0;
            int v2 = p2 != null ? p2.val : 0;
            int sum = carry + v1 + v2;
            carry = sum / 10;
            p3.next = new ListNode(sum % 10);
            p3 = p3.next;
            if (p1 != null) p1 = p1.next;
            if (p2 != null) p2 = p2.next;
        }
        if (carry == 1) {
            p3.next = new ListNode(carry);
        }
        return dummyHead.next;
    }
}
