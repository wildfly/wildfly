/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.affinity;

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
