package algorithm.leetcode;

import java.util.HashMap;
import java.util.Map;

/*
 * 交叉链表
 */
public class _160 {
    public ListNode getIntersectionNode(ListNode headA, ListNode headB) {
        ListNode p1 = headA;
        ListNode p2 = headB;
        if (p1 == null || p2 == null) {
            return null;
        }
        while (p1.next != null) {
            p2 = headB;
            while (p2.next != null) {
                if (p1 == p2) {
                    return p1;
                }
                p2 = p2.next;
            }
            p1 = p1.next;
        }
        return null;
    }

    /*
    Hash Table
     */
    public ListNode getIntersectionNode2(ListNode headA, ListNode headB) {
        ListNode p1 = headA;
        ListNode p2 = headB;
        Object o = new Object();
        Map<ListNode, Object> map = new HashMap<>();
        while (p1 != null) {
            map.put(p1, o);
            p1 = p1.next;
        }
        while (p2 != null) {
            if (map.get(p2) != null) {
                return p2;
            }
            p2 = p2.next;
        }
        return null;
    }
}
