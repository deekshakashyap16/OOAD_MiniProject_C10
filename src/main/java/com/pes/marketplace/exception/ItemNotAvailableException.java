package com.pes.marketplace.exception;

/** Thrown when a buyer attempts to purchase an unavailable item. */
public class ItemNotAvailableException extends RuntimeException {
    public ItemNotAvailableException(String msg) { super(msg); }
}
