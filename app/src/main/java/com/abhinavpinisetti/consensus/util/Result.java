package com.abhinavpinisetti.consensus.util;

/** Wraps a value with a loading/success/error state for LiveData. */
public final class Result<T> {

    public enum Status { LOADING, SUCCESS, ERROR }

    public final Status status;
    public final T data;
    public final String message;

    private Result(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> Result<T> loading() {
        return new Result<>(Status.LOADING, null, null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(Status.SUCCESS, data, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(Status.ERROR, null, message);
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }
}
