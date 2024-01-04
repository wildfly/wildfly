/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.scheduler;

import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.Scheduler;

/**
 * @author Paul Ferraro
 */
public class LocalSchedulerTestCase {

    @Test
    public void successfulTask() throws InterruptedException {
        ScheduledEntries<UUID, Instant> entries = mock(ScheduledEntries.class);
        Predicate<UUID> task = mock(Predicate.class);

        Map.Entry<UUID, Instant> entry = new SimpleImmutableEntry<>(UUID.randomUUID(), Instant.now());
        List<Map.Entry<UUID, Instant>> entryList = new ArrayList<>(1);
        entryList.add(entry);

        try (Scheduler<UUID, Instant> scheduler = new LocalScheduler<>(entries, task, Duration.ZERO)) {
            // Verify simple scheduling
            when(entries.peek()).thenReturn(entry, null);
            doAnswer(invocation -> entryList.iterator()).when(entries).iterator();
            when(task.test(entry.getKey())).thenReturn(true);

            scheduler.schedule(entry.getKey(), entry.getValue());

            verify(entries).add(entry.getKey(), entry.getValue());

            Thread.sleep(500);

            // Verify that entry was removed from backing collection
            Assert.assertTrue(entryList.isEmpty());
        }
    }

    @Test
    public void failingTask() throws InterruptedException {
        ScheduledEntries<UUID, Instant> entries = mock(ScheduledEntries.class);
        Predicate<UUID> task = mock(Predicate.class);

        Map.Entry<UUID, Instant> entry = new SimpleImmutableEntry<>(UUID.randomUUID(), Instant.now());
        List<Map.Entry<UUID, Instant>> entryList = new ArrayList<>(1);
        entryList.add(entry);

        try (Scheduler<UUID, Instant> scheduler = new LocalScheduler<>(entries, task, Duration.ZERO)) {
            // Verify that a failing scheduled task does not trigger removal
            when(entries.peek()).thenReturn(entry, null);
            doAnswer(invocation -> entryList.iterator()).when(entries).iterator();
            when(task.test(entry.getKey())).thenReturn(false);

            scheduler.schedule(entry.getKey(), entry.getValue());

            verify(entries).add(entry.getKey(), entry.getValue());

            Thread.sleep(500);

            // Verify that entry was not removed from backing collection
            Assert.assertFalse(entryList.isEmpty());
        }
    }

    @Test
    public void retryUntilSuccessfulTask() throws InterruptedException {
        ScheduledEntries<UUID, Instant> entries = mock(ScheduledEntries.class);
        Predicate<UUID> task = mock(Predicate.class);

        Map.Entry<UUID, Instant> entry = new SimpleImmutableEntry<>(UUID.randomUUID(), Instant.now());
        List<Map.Entry<UUID, Instant>> entryList = new ArrayList<>(1);
        entryList.add(entry);

        try (Scheduler<UUID, Instant> scheduler = new LocalScheduler<>(entries, task, Duration.ZERO)) {
            // Verify that a failing scheduled task does not trigger removal
            when(entries.peek()).thenReturn(entry, entry, null);
            doAnswer(invocation -> entryList.iterator()).when(entries).iterator();
            when(task.test(entry.getKey())).thenReturn(false, true);

            scheduler.schedule(entry.getKey(), entry.getValue());

            verify(entries).add(entry.getKey(), entry.getValue());

            Thread.sleep(500);

            // Verify that entry was eventually removed from backing collection
            Assert.assertTrue(entryList.isEmpty());
        }
    }

    @Test
    public void cancel() {
        ScheduledEntries<UUID, Instant> entries = mock(ScheduledEntries.class);
        Predicate<UUID> task = mock(Predicate.class);

        Map.Entry<UUID, Instant> entry = new SimpleImmutableEntry<>(UUID.randomUUID(), Instant.now());

        try (Scheduler<UUID, Instant> scheduler = new LocalScheduler<>(entries, task, Duration.ZERO)) {
            when(entries.peek()).thenReturn(entry);

            scheduler.cancel(entry.getKey());

            verify(entries).remove(entry.getKey());
        }
    }
}
