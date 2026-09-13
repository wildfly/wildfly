/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public class ServerLogTailerListenerTest {
    @Test
    public void linesAddedDuringIterationRemainForTheNextIteration() throws Exception {
        ServerLogTailerListener listener = new ServerLogTailerListener();
        CountDownLatch iterationStarted = new CountDownLatch(1);
        CountDownLatch allowIterationToComplete = new CountDownLatch(1);
        AtomicReference<Throwable> iterationFailure = new AtomicReference<>();
        List<String> firstIterationLines = new ArrayList<>();
        listener.handle("existing line");

        Thread reader = new Thread(() -> {
            try {
                listener.logs.stream().forEach(line -> {
                    firstIterationLines.add(line);
                    iterationStarted.countDown();
                    try {
                        assertTrue(allowIterationToComplete.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                });
            } catch (Throwable t) {
                iterationFailure.set(t);
            }
        });
        reader.start();

        assertTrue(iterationStarted.await(5, TimeUnit.SECONDS));
        listener.handle("line added during iteration");
        allowIterationToComplete.countDown();
        reader.join(5_000);

        assertDoesNotThrow(() -> {
            if (iterationFailure.get() != null) {
                throw new AssertionError(iterationFailure.get());
            }
        });
        assertFalse(firstIterationLines.contains("line added during iteration"));
        assertTrue(listener.logs.stream().anyMatch("line added during iteration"::equals));
    }
}
