package com.financetracker;

/**
 * Simple alert-sending contract.
 */
public interface Notification {
    void sendAlert(String message);
}