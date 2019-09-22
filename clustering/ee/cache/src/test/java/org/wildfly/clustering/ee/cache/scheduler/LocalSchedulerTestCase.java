/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.ee.cache.scheduler;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

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

        try (Scheduler<UUID, Instant> scheduler = new LocalScheduler<>(entries, task)) {
            // Verify simple scheduling
            when(entries.peek()).thenReturn(entry, null);
            doAnswer(invocation -> Collections.singleton(entry).iterator()).when(entries).iterator();
            when(task.test(entry.getKey())).thenReturn(true);

            scheduler.schedule(entry.getKey(), entry.getValue());

            Thread.sleep(500);

            verify(entries).add(entry.getKey(), entry.getValue());
            verify(entries).remove(entry.getKey());
        }
    }

    @Test
    public void failingTask() throws InterruptedException {
        ScheduledEntries<UUID, Instant> entries = mock(ScheduledEntries.class);
        Predicate<UUID> task = mock(Predicate.class);

        Map.Entry<UUID, Instant> entry = new SimpleImmutableEntry<>(UUID.randomUUID(), Instant.now());

        try (Scheduler<UUID, Instant> scheduler = new LocalScheduler<>(entries, task)) {
            // Verify that a failing scheduled task does not trigger removal
            when(entries.peek()).thenReturn(entry, entry, null);
            doAnswer(invocation -> Collections.singleton(entry).iterator()).when(entries).iterator();
            when(task.test(entry.getKey())).thenReturn(false, true);

            scheduler.schedule(entry.getKey(), entry.getValue());

            verify(entries).add(entry.getKey(), entry.getValue());

            Thread.sleep(500);

            verify(entries).add(entry.getKey(), entry.getValue());
            verify(entries).remove(entry.getKey());
        }
    }

    @Test
    public void cancel() {
        ScheduledEntries<UUID, Instant> entries = mock(ScheduledEntries.class);
        Predicate<UUID> task = mock(Predicate.class);

        Map.Entry<UUID, Instant> entry = new SimpleImmutableEntry<>(UUID.randomUUID(), Instant.now());

        try (Scheduler<UUID, Instant> scheduler = new LocalScheduler<>(entries, task)) {
            when(entries.peek()).thenReturn(entry);

            scheduler.cancel(entry.getKey());

            verify(entries).remove(entry.getKey());
        }
    }
}
