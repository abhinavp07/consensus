package com.abhinavpinisetti.consensus.data;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/** Turns exceptions into short, friendly messages for the UI. */
public final class ErrorMessages {

    public static final String GENERIC = "Something went wrong. Please try again.";
    public static final String OFFLINE = "You're offline. Check your connection and try again.";

    private ErrorMessages() {}

    public static String from(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof FriendlyException) return t.getMessage();
            if (t instanceof FirebaseNetworkException) return OFFLINE;
            if (t instanceof FirebaseFirestoreException) {
                switch (((FirebaseFirestoreException) t).getCode()) {
                    case UNAVAILABLE:
                    case DEADLINE_EXCEEDED:
                        return OFFLINE;
                    case PERMISSION_DENIED:
                        return "You don't have access to that.";
                    case NOT_FOUND:
                        return "That no longer exists.";
                    default:
                        return GENERIC;
                }
            }
            t = t.getCause();
        }
        return GENERIC;
    }

    /** Finds an exception of the given type anywhere in the cause chain. */
    public static <T extends Throwable> T find(Throwable e, Class<T> type) {
        Throwable t = e;
        while (t != null) {
            if (type.isInstance(t)) return type.cast(t);
            t = t.getCause();
        }
        return null;
    }

    /** An exception whose message is already safe to show to users. */
    public static class FriendlyException extends RuntimeException {
        public FriendlyException(String message) {
            super(message);
        }

        public FriendlyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
