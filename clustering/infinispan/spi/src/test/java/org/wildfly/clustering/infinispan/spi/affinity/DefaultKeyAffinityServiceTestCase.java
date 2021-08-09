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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.infinispan.AdvancedCache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.LocalizedCacheTopology;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.remoting.transport.Address;
import org.infinispan.topology.CacheTopology;
import org.infinispan.topology.PersistentUUID;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

/**
 * Unit test for {@link DefaultKeyAffinityService}.
 * @author Paul Ferraro
 */
public class DefaultKeyAffinityServiceTestCase {

    private static final int SEGMENTS = 3;
    private static final int LOCAL_SEGMENT = 0;
    private static final int REMOTE_SEGMENT = 1;
    private static final int FILTERED_SEGMENT = 2;

    @Test
    public void test() {
        KeyPartitioner partitioner = mock(KeyPartitioner.class);
        KeyGenerator<UUID> generator = mock(KeyGenerator.class);
        AdvancedCache<UUID, Object> cache = mock(AdvancedCache.class);
        Address local = mock(Address.class);
        Address remote = mock(Address.class);
        Address standby = mock(Address.class);
        Address ignored = mock(Address.class);
        KeyAffinityService<UUID> service = new DefaultKeyAffinityService<>(cache, partitioner, generator, address -> (address != ignored));

        DistributionManager dist = mock(DistributionManager.class);
        CacheTopology topology = mock(CacheTopology.class);
        ConsistentHash hash = mock(ConsistentHash.class);
        List<Address> members = Arrays.asList(local, remote, standby, ignored);

        when(cache.getAdvancedCache()).thenReturn(cache);
        when(cache.getDistributionManager()).thenReturn(dist);
        when(topology.getActualMembers()).thenReturn(members);
        when(topology.getCurrentCH()).thenReturn(hash);
        when(topology.getMembers()).thenReturn(members);
        when(topology.getMembersPersistentUUIDs()).thenReturn(Arrays.asList(PersistentUUID.randomUUID(), PersistentUUID.randomUUID(), PersistentUUID.randomUUID(), PersistentUUID.randomUUID()));
        when(topology.getPendingCH()).thenReturn(null);
        when(topology.getPhase()).thenReturn(CacheTopology.Phase.NO_REBALANCE);
        when(topology.getReadConsistentHash()).thenReturn(hash);
        when(topology.getRebalanceId()).thenReturn(0);
        when(topology.getTopologyId()).thenReturn(0);
        when(topology.getUnionCH()).thenReturn(null);
        when(topology.getWriteConsistentHash()).thenReturn(hash);
        when(hash.getMembers()).thenReturn(members);
        when(hash.getNumSegments()).thenReturn(SEGMENTS);
        when(hash.locatePrimaryOwnerForSegment(LOCAL_SEGMENT)).thenReturn(local);
        when(hash.locatePrimaryOwnerForSegment(REMOTE_SEGMENT)).thenReturn(remote);
        when(hash.locatePrimaryOwnerForSegment(FILTERED_SEGMENT)).thenReturn(ignored);
        when(hash.locateOwnersForSegment(LOCAL_SEGMENT)).thenReturn(Collections.singletonList(local));
        when(hash.locateOwnersForSegment(REMOTE_SEGMENT)).thenReturn(Collections.singletonList(remote));
        when(hash.locateOwnersForSegment(FILTERED_SEGMENT)).thenReturn(Collections.singletonList(ignored));
        when(hash.getPrimarySegmentsForOwner(local)).thenReturn(Collections.singleton(LOCAL_SEGMENT));
        when(hash.getPrimarySegmentsForOwner(remote)).thenReturn(Collections.singleton(REMOTE_SEGMENT));
        when(hash.getPrimarySegmentsForOwner(standby)).thenReturn(Collections.emptySet());
        when(hash.getPrimarySegmentsForOwner(ignored)).thenReturn(Collections.singleton(FILTERED_SEGMENT));
        when(hash.getSegmentsForOwner(local)).thenReturn(Collections.singleton(LOCAL_SEGMENT));
        when(hash.getSegmentsForOwner(remote)).thenReturn(Collections.singleton(REMOTE_SEGMENT));
        when(hash.getSegmentsForOwner(standby)).thenReturn(Collections.emptySet());
        when(hash.getSegmentsForOwner(ignored)).thenReturn(Collections.singleton(FILTERED_SEGMENT));

        LocalizedCacheTopology localizedTopology = new LocalizedCacheTopology(CacheMode.DIST_SYNC, topology, partitioner, local, true);

        when(dist.getCacheTopology()).thenReturn(localizedTopology);

        // Mock a sufficient number of keys
        OngoingStubbing<UUID> stub = when(generator.getKey());
        for (int i = 0; i < 1000; ++i) {
            UUID key = UUID.randomUUID();
            int segment = getSegment(key);

            stub = stub.thenReturn(key);

            when(partitioner.getSegment(key)).thenReturn(segment);
        }

        assertThrows(IllegalStateException.class, () -> service.getKeyForAddress(local));
        assertThrows(IllegalStateException.class, () -> service.getKeyForAddress(remote));
        assertThrows(IllegalStateException.class, () -> service.getKeyForAddress(standby));
        // This should throw IAE, since address does not pass filter
        assertThrows(IllegalArgumentException.class, () -> service.getKeyForAddress(ignored));

        service.start();

        try {
            for (int i = 0; i < 50; ++i) {
                UUID key = service.getKeyForAddress(local);
                int segment = getSegment(key);
                assertEquals(LOCAL_SEGMENT, segment);

                key = service.getCollocatedKey(key);
                segment = getSegment(key);
                assertEquals(LOCAL_SEGMENT, segment);

                key = service.getKeyForAddress(remote);
                segment = getSegment(key);
                assertEquals(REMOTE_SEGMENT, segment);

                key = service.getCollocatedKey(key);
                segment = getSegment(key);
                assertEquals(REMOTE_SEGMENT, segment);
            }

            // This should return a random key
            assertNotNull(service.getKeyForAddress(standby));
            // This should throw IAE, since address does not pass filter
            assertThrows(IllegalArgumentException.class, () -> service.getKeyForAddress(ignored));
        } finally {
            service.stop();
        }
    }

    private static int getSegment(UUID key) {
        return Math.abs(key.hashCode()) % SEGMENTS;
    }
}
