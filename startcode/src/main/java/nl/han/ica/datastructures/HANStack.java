package nl.han.ica.datastructures;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

public class HANStack<T> implements IHANStack<T> {

    private final ArrayDeque<T> stack = new ArrayDeque<>();


    @Override
    public void push(T value) {
        stack.push(value);
    }

    @Override
    public T pop() {
        if (stack.isEmpty()) {
            throw new NoSuchElementException("Empty stack");
        }
        return stack.pop();
    }

    @Override
    public T peek() {
        if (stack.isEmpty()) {
            throw new NoSuchElementException("Empty stack");
        }
        return stack.peek();
    }
}
