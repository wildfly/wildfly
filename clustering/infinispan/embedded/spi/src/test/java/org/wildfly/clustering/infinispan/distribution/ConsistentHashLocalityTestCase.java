/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.distribution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.infinispan.remoting.transport.Address;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ConsistentHashLocalityTestCase {

    @Test
    public void test() {
        KeyDistribution distribution = mock(KeyDistribution.class);
        Address localAddress = mock(Address.class);
        Address remoteAddress = mock(Address.class);
        Object localKey = new Object();
        Object remoteKey = new Object();

        Locality locality = new ConsistentHashLocality(distribution, localAddress);

        when(distribution.getPrimaryOwner(localKey)).thenReturn(localAddress);
        when(distribution.getPrimaryOwner(remoteKey)).thenReturn(remoteAddress);

        assertTrue(locality.isLocal(localKey));
        assertFalse(locality.isLocal(remoteKey));
    }
}
