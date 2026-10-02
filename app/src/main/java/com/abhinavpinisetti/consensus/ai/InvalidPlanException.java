package com.abhinavpinisetti.consensus.ai;

/** Thrown when the AI response is malformed or incomplete. Nothing should be saved. */
public class InvalidPlanException extends Exception {
    public InvalidPlanException(String message) {
        super(message);
    }

    public InvalidPlanException(String message, Throwable cause) {
        super(message, cause);
    }
}
