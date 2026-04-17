package com.pes.marketplace.exception;

/** Thrown when a user attempts an action they are not permitted to perform. */
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String msg) { super(msg); }
}
