package algorithms;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;




public class PrefixSumSentiment{


    // This solution is copied from the wpo exercise on prefixsum.

    public static class SumTask extends RecursiveAction {
        private static final long  serialVersionUID = -2039136112928941061L;
        private final TempTreeNode treeNode;
        private final Float          leftSum;
        private final Float[]        input;
        private final Float[]        result;

        public SumTask(TempTreeNode treeNode, Float leftSum, Float[] input, Float[] result) {
            this.treeNode = treeNode;
            this.leftSum = leftSum;
            this.input = input;
            this.result = result;
        }

        @Override
        protected void compute() {
            if (treeNode.isLeaf()) {
                result[treeNode.lo] = input[treeNode.lo] + leftSum;
            } else {
                SumTask left = new SumTask(treeNode.leftNode, leftSum, input, result);
                SumTask right = new SumTask(treeNode.rightNode, leftSum + treeNode.leftNode.sum, input, result);

                left.fork();
                right.compute();
                left.join();
            }
        }
    }

    public static class BuildNodeTask extends RecursiveTask<TempTreeNode> {
        private static final long serialVersionUID = -742809860551941054L;
        private final Float[]     input;
        private final int         lo;
        private final int         hi;

        public BuildNodeTask(Float [] input, int lo, int hi) {
            this.input = input;
            this.lo = lo;
            this.hi = hi;
        }

        @Override
        protected TempTreeNode compute() {
            if (hi - lo < 2) {
                return new TempTreeNode(null, null, input[lo], lo, hi);
            } else {
                BuildNodeTask left  = new BuildNodeTask(input, lo,            (hi + lo) / 2);
                BuildNodeTask right = new BuildNodeTask(input, (hi + lo) / 2,            hi);

                left.fork();

                TempTreeNode rightTreeNode = right.compute();
                TempTreeNode leftTreeNode  = left.join();
                return new TempTreeNode(leftTreeNode, rightTreeNode,leftTreeNode.sum + rightTreeNode.sum, lo, hi);
            }
        }
    }

    public static class TempTreeNode {
        public final TempTreeNode leftNode;
        public final TempTreeNode rightNode;

        public final Float          sum;

        public final int          lo;
        public final int          hi;

        TempTreeNode(TempTreeNode left, TempTreeNode right, Float sum, int lo, int hi) {
            leftNode = left;
            rightNode = right;

            this.sum = sum;

            this.lo = lo;
            this.hi = hi;
        }

        public boolean isLeaf() {
            return leftNode == null && rightNode == null;
        }
    }



}

