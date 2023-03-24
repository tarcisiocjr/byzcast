package ch.usi.inf.dslab.byzcast.util;

import java.util.ArrayList;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin@acad.pucrs.br
 */

public class Tree<T> {
    private final T val;
    private final ArrayList<Tree<T>> children;

    public Tree(T val) {
        this.val = val;
        children = new ArrayList<>();
    }

    public Tree<T> addChild(T child) {
        var c = new Tree<T>(child);
        children.add(c);
        return c;
    }

    @Override
    public String toString() {
        return "Tree{" +
                "val=" + val +
                ", children=" + children +
                '}';
    }

    public T getValue() {
        return this.val;
    }

    public ArrayList<Tree<T>> getChildren() {
        return this.children;
    }
}
