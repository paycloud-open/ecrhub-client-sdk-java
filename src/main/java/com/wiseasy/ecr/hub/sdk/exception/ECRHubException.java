package com.wiseasy.ecr.hub.sdk.exception;

public class ECRHubException extends Exception {

    public ECRHubException() {
        super();
    }

    public ECRHubException(String message) {
        super(message);
    }

    public ECRHubException(Throwable cause) {
        super(cause);
    }

    public ECRHubException(String message, Throwable cause) {
        super(message, cause);
    }

}