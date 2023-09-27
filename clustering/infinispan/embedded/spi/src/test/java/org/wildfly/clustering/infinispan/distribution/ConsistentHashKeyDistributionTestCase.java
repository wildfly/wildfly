/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.distribution;

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
