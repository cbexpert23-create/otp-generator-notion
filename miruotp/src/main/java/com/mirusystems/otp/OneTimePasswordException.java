package com.mirusystems.otp;

public class OneTimePasswordException extends Exception {

    public OneTimePasswordException() {
        super();
    }

    public OneTimePasswordException(String s) {
        super(s);
    }

    public OneTimePasswordException(String message, Throwable cause) {
        super(message, cause);
    }

    public OneTimePasswordException(Throwable cause) {
        super(cause);
    }
}
