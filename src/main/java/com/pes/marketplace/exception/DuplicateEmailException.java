package com.pes.marketplace.exception;

/** Thrown when a registration email is already in use. */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String msg) { super(msg); }
}
