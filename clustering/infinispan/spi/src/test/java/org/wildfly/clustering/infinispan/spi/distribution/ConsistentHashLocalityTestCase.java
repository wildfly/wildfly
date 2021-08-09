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
