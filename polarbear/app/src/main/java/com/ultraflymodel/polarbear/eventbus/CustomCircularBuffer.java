package com.ultraflymodel.polarbear.eventbus;

import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;

/**
 * Created by lghost2018 on 2018/4/10.
 */


public class CustomCircularBuffer<T> {

    private T[] buffer;

    private int tail;

    private int head;


    public CustomCircularBuffer(int n) {
        buffer = (T[]) new Object[n];
        tail = 0;
        head = 0;
    }

    public void add(T toAdd) {
/*
        if (head != (tail - 1)) {
            buffer[head++] = toAdd;
        } else {
            throw new BufferOverflowException();
        }
        head = head % buffer.length;
   */
        buffer[head++] = toAdd;
        if (head == buffer.length)
        {
            head = head -buffer.length;
            tail = head + 1;
        }
        else
        {
            tail = head;
        }


    }

    public T get() {
        T t = null;
        int adjTail = tail > head ? tail - buffer.length : tail;

        t = (T) buffer[tail++];
        tail = tail % buffer.length;

/*
        if (adjTail < head) {
            t = (T) buffer[tail++];
            tail = tail % buffer.length;
        } else {
            throw new BufferUnderflowException();
        }
*/
        return t;
    }

    public String toString() {
        return "CustomCircularBuffer(size=" + buffer.length + ", head=" + head + ", tail=" + tail + ")";
    }
}
