package com.example.hotelbooking.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Shared by all inventory operations in this single-process implementation. */
@Component
public class InventoryLock {
    private final ReentrantLock lock = new ReentrantLock();

    public <T> T execute(Supplier<T> operation) {
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }
}
