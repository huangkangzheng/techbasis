package algorithm.leetcode;

import algorithm.ListNode;

import java.util.*;

/*
 * 重排链表
 */
public class _143 {
    public void reorderList(ListNode head) {
        ListNode p1 = head;
        Stack<ListNode> stack = new Stack<>();
        Queue<ListNode> queue = new LinkedList<>();
        int size = 0;
        while (p1 != null) {
            stack.push(p1);
            queue.offer(p1);
            p1 = p1.next;
            size++;
        }
        size = size / 2;
        while (size > 0) {
        }
    }
}
