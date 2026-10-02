package com.abhinavpinisetti.consensus.util;

/** A LiveData payload that should be handled once (navigation, toasts). */
public final class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    /** Returns the content the first time it's called, then null. */
    public T consume() {
        if (handled) return null;
        handled = true;
        return content;
    }

    public T peek() {
        return content;
    }
}
