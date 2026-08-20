package com.yumegod.obfuscator.jobf.utils;

import java.util.LinkedList;

// lite version of LinkedList, primitive type "byte" specific
@SuppressWarnings("unused")
public class ByteLinkedList {

    transient int size = 0;

    /**
     * Pointer to first node.
     * Invariant: (first == null && last == null) ||
     *            (first.prev == null && first.item != null)
     */
    transient ByteLinkedListNode first;

    /**
     * Pointer to last node.
     * Invariant: (first == null && last == null) ||
     *            (last.next == null && last.item != null)
     */
    transient ByteLinkedListNode last;

    // link/unlink
    private void linkFirst(byte value) {
        ByteLinkedListNode f = first;
        ByteLinkedListNode newNode = new ByteLinkedListNode(value, f, null);
        first = newNode;
        if (f == null)
            last = newNode;
        else
            f.prev = newNode;
        size++;
    }

    private void linkLast(byte value) {
        ByteLinkedListNode l = last;
        ByteLinkedListNode newNode = new ByteLinkedListNode(value, null, l);
        last = newNode;
        if (l == null)
            first = newNode;
        else
            l.next = newNode;
        size++;
    }

    private void linkBefore(byte value, ByteLinkedListNode succ) {
        ByteLinkedListNode pred = succ.prev;
        ByteLinkedListNode newNode = new ByteLinkedListNode(value, succ, pred);
        succ.prev = newNode;
        if (pred == null)
            first = newNode;
        else
            pred.next = newNode;
        size++;
    }

    private void linkAfter(byte value, ByteLinkedListNode pred) {
        ByteLinkedListNode succ = pred.next;
        ByteLinkedListNode newNode = new ByteLinkedListNode(value, succ, pred);
        pred.next = newNode;
        if (succ == null)
            last = newNode;
        else
            succ.prev = newNode;
        size++;
    }

    private byte unlink(ByteLinkedListNode x) {
        ByteLinkedListNode next = x.next;
        ByteLinkedListNode prev = x.prev;
        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
        }
        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
        }
        size--;
        return x.value;
    }

    public byte get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        ByteLinkedListNode node;
        if (index < (size >> 1)) {
            node = first;
            for (int i = 0; i < index; i++) {
                node = node.next;
            }
        } else {
            node = last;
            for (int i = size - 1; i > index; i--) {
                node = node.prev;
            }
        }
        return node.value;
    }

    // add last
    public ByteLinkedList add(byte value) {
        linkLast(value);
        return this;
    }

    // add first
    public void addFirst(byte value) {
        linkFirst(value);
    }

    // add all
    public void addAll(byte[] values) {
        for (byte value : values) {
            linkLast(value);
        }
    }

    // get last
    public byte getLast() {
        if (size == 0) {
            throw new IllegalStateException("List is empty");
        }
        return last.value;
    }

    // remove last
    public byte removeLast() {
        if (size == 0) {
            throw new IllegalStateException("List is empty");
        }
        return unlink(last);
    }

    public byte removeFirst() {
        if (size == 0) {
            throw new IllegalStateException("List is empty");
        }
        return unlink(first);
    }

    // recalculate size
    public void recalculateSize() {
        int size = 0;
        for (ByteLinkedListNode node = first; node != null; node = node.next) {
            size++;
        }
        this.size = size;
    }

    public byte[] toArray() {
        byte[] array = new byte[size];
        int i = 0;
        for (ByteLinkedListNode node = first; node != null; node = node.next) {
            array[i++] = node.value;
        }
        return array;
    }

    // iter
    public LinkedList<Byte> iter() {
        LinkedList<Byte> list = new LinkedList<>();
        for (ByteLinkedListNode node = first; node != null; node = node.next) {
            list.add(node.value);
        }
        return list;
    }

    public boolean contains(byte value) {
        for (ByteLinkedListNode node = first; node != null; node = node.next) {
            if (node.value == value) {
                return true;
            }
        }
        return false;
    }


    public static class ByteLinkedListNode {
        public byte value;
        public ByteLinkedListNode next;
        public ByteLinkedListNode prev;
        public ByteLinkedListNode(byte value) {
            this.value = value;
        }
        public ByteLinkedListNode(byte value, ByteLinkedListNode next, ByteLinkedListNode prev) {
            this.value = value;
            this.next = next;
            this.prev = prev;
        }
    }
}
