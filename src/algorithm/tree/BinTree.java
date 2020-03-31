package algorithm.tree;

import algorithm.TreeNode;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/*
 * 二叉树先序，中序，后序，层级遍历算法
 */
public class BinTree {
    public void preOrder(TreeNode root) {
        if (root == null)
            return;
        System.out.println(root.val + " ");
        preOrder(root.left);
        preOrder(root.right);
    }

    public void preOrder1(TreeNode root) {
        Stack<TreeNode> stack = new Stack<>();
        while (root != null || !stack.isEmpty()) {
            while (root != null) {
                System.out.println(root.val + " ");
                stack.push(root);
                root = root.left;
            }
            if (!stack.isEmpty()) {
                root = stack.pop();
                root = root.right;
            }
        }
    }

    public void inOrder(TreeNode root) {
        if (root == null)
            return;
        inOrder(root.left);
        System.out.println(root.val + " ");
        inOrder(root.right);
    }

    public void inOrder1(TreeNode root) {
        Stack<TreeNode> stack = new Stack<>();
        while (root != null || !stack.isEmpty()) {
            while (root != null) {
                stack.push(root);
                root = root.left;
            }
            if (!stack.isEmpty()) {
                root = stack.pop();
                System.out.println(root.val + " ");
                root = root.right;
            }
        }
    }

    public void postOrder(TreeNode root) {
        if (root == null)
            return;
        postOrder(root.left);
        postOrder(root.right);
        System.out.println(root.val + " ");
    }

    public void levelOrder(TreeNode root) {
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            TreeNode p = queue.poll();
            System.out.println(p.val + " ");
            if (p.left != null) {
                queue.offer(p.left);
            }
            if (p.right != null) {
                queue.offer(p.right);
            }
        }
    }
}
