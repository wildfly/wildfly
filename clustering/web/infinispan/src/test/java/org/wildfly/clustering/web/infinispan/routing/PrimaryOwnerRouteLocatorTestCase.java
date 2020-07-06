/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.routing;

import static org.mockito.Mockito.*;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.LocalizedCacheTopology;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.topology.CacheTopology;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * Unit test for {@link InfinispanRouteLocator}.
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class PrimaryOwnerRouteLocatorTestCase {

    @Parameters
    public static Iterable<CacheMode> parameters() {
        return EnumSet.allOf(CacheMode.class).stream().filter(CacheMode::isSynchronous).collect(Collectors.toList());
    }

    private final Address[] addresses = new Address[] { mock(Address.class), mock(Address.class), mock(Address.class) };
    private final Address localAddress = mock(Address.class);
    private final Node[] members = new Node[] { mock(Node.class), mock(Node.class), mock(Node.class) };
    private final Node localMember = mock(Node.class);
    private final AdvancedCache<String, ?> cache = mock(AdvancedCache.class);
    private final DistributionManager dist = mock(DistributionManager.class);
    private final NodeFactory<Address> factory = mock(NodeFactory.class);
    private final Registry<String, Void> registry = mock(Registry.class);
    private final Group group = mock(Group.class);
    private final KeyPartitioner partitioner = mock(KeyPartitioner.class);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public PrimaryOwnerRouteLocatorTestCase(CacheMode mode) {
        EmbeddedCacheManager manager = mock(EmbeddedCacheManager.class);
        Configuration config = new ConfigurationBuilder().clustering().cacheMode(mode).build();
        when(this.cache.getCacheManager()).thenReturn(manager);
        when(manager.getAddress()).thenReturn(this.localAddress);
        when(this.cache.getCacheConfiguration()).thenReturn(config);
        when(this.cache.getAdvancedCache()).thenReturn((AdvancedCache) this.cache);
        when(this.cache.getDistributionManager()).thenReturn(this.dist);
        ConsistentHash hash = mock(ConsistentHash.class);
        when(hash.getMembers()).thenReturn(Arrays.asList(this.addresses));
        when(hash.getNumSegments()).thenReturn(3);
        when(hash.isReplicated()).thenReturn(mode.isReplicated());
        // Segment 0, local is not an owner
        when(hash.locatePrimaryOwnerForSegment(0)).thenReturn(this.addresses[0]);
        when(hash.locateOwnersForSegment(0)).thenReturn(mode.isDistributed() ? Arrays.asList(this.addresses).subList(0, 2) : Arrays.asList(this.addresses[0], this.addresses[1], this.addresses[2]));
        // Segment 1, local is primary owner
        when(hash.locatePrimaryOwnerForSegment(1)).thenReturn(this.addresses[1]);
        when(hash.locateOwnersForSegment(1)).thenReturn(mode.isDistributed() ? Arrays.asList(this.addresses).subList(1, 3) : Arrays.asList(this.addresses[1], this.addresses[2], this.addresses[0]));
        // Segment 2, local is a backup owner
        when(hash.locatePrimaryOwnerForSegment(2)).thenReturn(this.addresses[2]);
        when(hash.locateOwnersForSegment(2)).thenReturn(mode.isDistributed() ? Arrays.asList(this.addresses[2], this.addresses[0]) : Arrays.asList(this.addresses[2], this.addresses[0], this.addresses[1]));
        CacheTopology topology = new CacheTopology(1, 1, hash, null, CacheTopology.Phase.NO_REBALANCE, hash.getMembers(), null);
        LocalizedCacheTopology localizedTopology = new LocalizedCacheTopology(mode, topology, this.partitioner, manager.getAddress(), true);
        when(this.dist.getCacheTopology()).thenReturn(localizedTopology);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void test() {
        PrimaryOwnerRouteLocatorConfiguration config = mock(PrimaryOwnerRouteLocatorConfiguration.class);

        when(config.getCache()).thenReturn((AdvancedCache) this.cache);
        when(config.getMemberFactory()).thenReturn(this.factory);
        when(config.getRegistry()).thenReturn(this.registry);
        for (int i = 0; i < this.addresses.length; ++i) {
            when(this.factory.createNode(this.addresses[i])).thenReturn(this.members[i]);
            when(this.registry.getEntry(this.members[i])).thenReturn(new AbstractMap.SimpleImmutableEntry<>(String.valueOf(i), null));
        }
        when(this.registry.getGroup()).thenReturn(this.group);
        when(this.group.getLocalMember()).thenReturn(this.localMember);
        when(this.registry.getEntry(this.localMember)).thenReturn(new AbstractMap.SimpleImmutableEntry<>("local", null));

        RouteLocator locator = new PrimaryOwnerRouteLocator(config);

        switch (this.cache.getCacheConfiguration().clustering().cacheMode()) {
            case INVALIDATION_SYNC:
            case REPL_SYNC:
            case DIST_SYNC:
            case SCATTERED_SYNC: {
                when(this.partitioner.getSegment(new Key<>("session"))).thenReturn(0);
                String result = locator.locate("session");
                Assert.assertEquals("0", result);

                when(this.partitioner.getSegment(new Key<>("session"))).thenReturn(1);
                result = locator.locate("session");
                Assert.assertEquals("1", result);

                when(this.partitioner.getSegment(new Key<>("session"))).thenReturn(2);
                result = locator.locate("session");
                Assert.assertEquals("2", result);
                break;
            }
            default: {
                String result = locator.locate("session");
                Assert.assertEquals("local", result);
            }
        }
    }
}
