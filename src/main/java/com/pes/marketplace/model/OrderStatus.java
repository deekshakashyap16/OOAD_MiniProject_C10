package com.pes.marketplace.model;

/**
 * State enum for the Order lifecycle.
 *
 * State Diagram:
 *   PLACED ──confirm──▶ CONFIRMED ──complete──▶ COMPLETED
 *                    ──cancel──▶  CANCELLED
 */
public enum OrderStatus {
    PLACED,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
