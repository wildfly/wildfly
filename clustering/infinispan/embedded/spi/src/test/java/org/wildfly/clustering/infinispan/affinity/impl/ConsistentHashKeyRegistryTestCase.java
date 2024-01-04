/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.affinity.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.remoting.transport.Address;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ConsistentHashKeyRegistryTestCase {

    @Test
    public void test() {
        ConsistentHash hash = mock(ConsistentHash.class);
        Predicate<Address> filter = mock(Predicate.class);
        Supplier<BlockingQueue<Object>> queueFactory = mock(Supplier.class);
        Address local = mock(Address.class);
        Address filtered = mock(Address.class);
        Address standby = mock(Address.class);
        BlockingQueue<Object> queue = mock(BlockingQueue.class);

        when(hash.getMembers()).thenReturn(Arrays.asList(local, filtered, standby));
        when(filter.test(local)).thenReturn(true);
        when(filter.test(filtered)).thenReturn(false);
        when(filter.test(standby)).thenReturn(true);
        when(hash.getPrimarySegmentsForOwner(local)).thenReturn(Collections.singleton(1));
        when(hash.getPrimarySegmentsForOwner(standby)).thenReturn(Collections.emptySet());
        when(queueFactory.get()).thenReturn(queue);

        KeyRegistry<Object> registry = new ConsistentHashKeyRegistry<>(hash, filter, queueFactory);

        assertTrue(registry.getAddresses().contains(local));
        assertFalse(registry.getAddresses().contains(filtered));
        assertFalse(registry.getAddresses().contains(standby));
        assertEquals(Collections.singleton(local), registry.getAddresses());
        assertSame(queue, registry.getKeys(local));
        assertNull(registry.getKeys(standby));
        assertNull(registry.getKeys(filtered));
    }
}
