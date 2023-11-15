package com.wiseasy.ecr.hub.sdk.exception;

public class ECRHubTimeoutException extends ECRHubException {

    public ECRHubTimeoutException() {
        super();
    }

    public ECRHubTimeoutException(String message) {
        super(message);
    }

    public ECRHubTimeoutException(Throwable cause) {
        super(cause);
    }

    public ECRHubTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}