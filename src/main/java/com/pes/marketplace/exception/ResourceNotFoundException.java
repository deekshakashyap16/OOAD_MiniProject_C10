package com.pes.marketplace.exception;

/** Thrown when a requested resource does not exist in the database. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
}
