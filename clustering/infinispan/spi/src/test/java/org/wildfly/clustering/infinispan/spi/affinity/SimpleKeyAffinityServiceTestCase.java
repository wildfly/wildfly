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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.remoting.transport.Address;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class SimpleKeyAffinityServiceTestCase {

    private final KeyGenerator<UUID> generator = mock(KeyGenerator.class);
    private final KeyAffinityService<UUID> service = new SimpleKeyAffinityService<>(this.generator);

    @Test
    public void getKeyForAddress() {
        Address address = mock(Address.class);
        UUID key = UUID.randomUUID();

        when(this.generator.getKey()).thenReturn(key);

        UUID result = this.service.getKeyForAddress(address);

        assertSame(key, result);
    }

    @Test
    public void getCollocatedKey() {
        UUID key = UUID.randomUUID();

        when(this.generator.getKey()).thenReturn(key);

        UUID result = this.service.getCollocatedKey(UUID.randomUUID());

        assertSame(key, result);
    }
}
