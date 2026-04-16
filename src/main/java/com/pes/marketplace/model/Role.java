package com.pes.marketplace.model;

/**
 * GRASP – Information Expert: Role knows its own string representation.
 * SRP: Only responsibility is defining the three access levels.
 */
public enum Role {
    BUYER,
    SELLER,
    ADMIN
}
