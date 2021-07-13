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

package org.wildfly.clustering.infinispan.spi.distribution;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.remoting.transport.Address;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ConsistentHashKeyDistributionTestCase {

    @Test
    public void test() {
        KeyPartitioner partitioner = mock(KeyPartitioner.class);
        ConsistentHash hash = mock(ConsistentHash.class);
        KeyDistribution distribution = new ConsistentHashKeyDistribution(partitioner, hash);

        Address address = mock(Address.class);
        Object key = new Object();
        int segment = 128;

        when(partitioner.getSegment(key)).thenReturn(segment);
        when(hash.locatePrimaryOwnerForSegment(segment)).thenReturn(address);

        Address result = distribution.getPrimaryOwner(key);

        assertSame(address, result);
    }
}
