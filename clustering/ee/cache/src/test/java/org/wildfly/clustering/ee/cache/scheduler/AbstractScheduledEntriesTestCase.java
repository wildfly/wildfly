/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.function.UnaryOperator;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractScheduledEntriesTestCase {

    private final ScheduledEntries<UUID, Instant> entrySet;
    private final UnaryOperator<List<Map.Entry<UUID, Instant>>> expectedFactory;

    AbstractScheduledEntriesTestCase(ScheduledEntries<UUID, Instant> entrySet, UnaryOperator<List<Map.Entry<UUID, Instant>>> expectedFactory) {
        this.entrySet = entrySet;
        this.expectedFactory = expectedFactory;
    }

    @Test
    public void test() {
        // Verify empty
        Assert.assertFalse(this.entrySet.iterator().hasNext());

        // Populate
        List<Map.Entry<UUID, Instant>> entries = new LinkedList<>();
        Instant now = Instant.now();
        entries.add(new SimpleImmutableEntry<>(UUID.randomUUID(), now));
        entries.add(new SimpleImmutableEntry<>(UUID.randomUUID(), now));
        entries.add(new SimpleImmutableEntry<>(UUID.randomUUID(), now.minus(Duration.ofSeconds(1))));
        entries.add(new SimpleImmutableEntry<>(UUID.randomUUID(), now.plus(Duration.ofSeconds(2))));
        entries.add(new SimpleImmutableEntry<>(UUID.randomUUID(), now.plus(Duration.ofSeconds(1))));

        for (Map.Entry<UUID, Instant> entry : entries) {
            this.entrySet.add(entry.getKey(), entry.getValue());
        }

        List<Map.Entry<UUID, Instant>> expected = this.expectedFactory.apply(entries);
        Assert.assertEquals(5, expected.size());

        // Verify iteration order corresponds to expected order
        Iterator<Map.Entry<UUID, Instant>> iterator = this.entrySet.iterator();
        for (Map.Entry<UUID, Instant> entry : expected) {
            Assert.assertTrue(iterator.hasNext());
            Map.Entry<UUID, Instant> result = iterator.next();
            Assert.assertSame(entry.getKey(), result.getKey());
            Assert.assertSame(entry.getValue(), result.getValue());
        }
        Assert.assertFalse(iterator.hasNext());

        // Verify iteration order after removal of first item
        this.entrySet.remove(expected.remove(0).getKey());

        // Verify iteration order corresponds to expected order
        iterator = this.entrySet.iterator();
        for (Map.Entry<UUID, Instant> entry : expected) {
            Assert.assertTrue(iterator.hasNext());
            Map.Entry<UUID, Instant> result = iterator.next();
            Assert.assertSame(entry.getKey(), result.getKey());
            Assert.assertSame(entry.getValue(), result.getValue());
        }
        Assert.assertFalse(iterator.hasNext());

        // Verify iteration order after removal of middle item
        this.entrySet.remove(expected.remove((expected.size() - 1) / 2).getKey());

        // Verify iteration order corresponds to expected order
        iterator = this.entrySet.iterator();
        for (Map.Entry<UUID, Instant> entry : expected) {
            Assert.assertTrue(iterator.hasNext());
            Map.Entry<UUID, Instant> result = iterator.next();
            Assert.assertSame(entry.getKey(), result.getKey());
            Assert.assertSame(entry.getValue(), result.getValue());
        }
        Assert.assertFalse(iterator.hasNext());

        // Verify iteration order after removal of last item
        this.entrySet.remove(expected.remove((expected.size() - 1)).getKey());

        // Verify iteration order corresponds to expected order
        iterator = this.entrySet.iterator();
        for (Map.Entry<UUID, Instant> entry : expected) {
            Assert.assertTrue(iterator.hasNext());
            Map.Entry<UUID, Instant> result = iterator.next();
            Assert.assertSame(entry.getKey(), result.getKey());
            Assert.assertSame(entry.getValue(), result.getValue());
        }
        Assert.assertFalse(iterator.hasNext());

        // Verify removal of non-existent entry
        this.entrySet.remove(UUID.randomUUID());
    }
}
