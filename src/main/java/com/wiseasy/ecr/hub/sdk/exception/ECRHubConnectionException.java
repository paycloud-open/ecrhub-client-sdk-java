package com.wiseasy.ecr.hub.sdk.exception;

public class ECRHubConnectionException extends ECRHubException {

    public ECRHubConnectionException() {
        super();
    }

    public ECRHubConnectionException(String message) {
        super(message);
    }

    public ECRHubConnectionException(Throwable cause) {
        super(cause);
    }

    public ECRHubConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}