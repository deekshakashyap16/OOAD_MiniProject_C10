package com.pes.marketplace.model;

/**
 * State enum for the Item lifecycle (State Design Pattern).
 *
 * GRASP – Information Expert: ItemStatus knows all valid states an item can be in.
 * SRP: Only defines item states; transition logic lives in ItemService.
 *
 * State Diagram transitions:
 *   PENDING_REVIEW ──approve──▶ APPROVED ──buy──▶ SOLD
 *   PENDING_REVIEW ──reject──▶ REJECTED ──resubmit──▶ PENDING_REVIEW
 *   APPROVED / REJECTED ──seller removes──▶ REMOVED
 */
public enum ItemStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    SOLD,
    REMOVED
}
