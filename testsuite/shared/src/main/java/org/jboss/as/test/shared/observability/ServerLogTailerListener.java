/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

/** Captures tailed server log lines and exposes asynchronous tailer failures. */
public class ServerLogTailerListener implements TailerListener {
    public final List<String> logs = new CopyOnWriteArrayList<>();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    /** Records that the configured server log file was not found. */
    @Override
    public void fileNotFound() {
        failure.compareAndSet(null, new IllegalStateException("Server log file was not found"));
    }

    /** Clears captured lines when the server rotates its log file. */
    @Override
    public void fileRotated() {
        logs.clear();
    }

    /** Records an asynchronous tailer exception for the test thread. */
    @Override
    public void handle(Exception exception) {
        failure.compareAndSet(null, exception);
    }

    /** Captures one line read from the server log. */
    @Override
    public void handle(String line) {
        logs.add(line);
    }

    /** Handles tailer initialization; no initialization state is required. */
    @Override
    public void init(Tailer tailer) {
    }

    /** Fails the calling test if the tailer reported an asynchronous error. */
    public void assertNoFailure() {
        Throwable failure = this.failure.get();
        if (failure != null) {
            throw new AssertionError("Server log tailer failed", failure);
        }
    }
}
