/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.infinispan.affinity;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.cache.IdentifierFactory;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;

/**
 * Unit test for {@link AffinityIdentifierFactory}
 *
 * @author Paul Ferraro
 */
public class AffinityIdentifierFactoryTestCase {

    private final Supplier<UUID> factory = mock(Supplier.class);
    private final KeyAffinityServiceFactory affinityFactory = mock(KeyAffinityServiceFactory.class);
    private final KeyAffinityService<Key<UUID>> affinity = mock(KeyAffinityService.class);
    private final Cache<Key<UUID>, ?> cache = mock(Cache.class);
    private final Address localAddress = mock(Address.class);

    private IdentifierFactory<UUID> subject;

    @Captor
    private ArgumentCaptor<KeyGenerator<Key<UUID>>> capturedGenerator;

    @Before
    public void init() throws Exception {
        EmbeddedCacheManager manager = mock(EmbeddedCacheManager.class);
        try (AutoCloseable test = MockitoAnnotations.openMocks(this)) {
            when(this.affinityFactory.createService(same(this.cache), this.capturedGenerator.capture())).thenReturn(this.affinity);
            when(this.cache.getCacheManager()).thenReturn(manager);
            when(manager.getAddress()).thenReturn(this.localAddress);

            this.subject = new AffinityIdentifierFactory<>(this.factory, this.cache, this.affinityFactory);

            KeyGenerator<Key<UUID>> generator = this.capturedGenerator.getValue();

            assertSame(generator, this.subject);

            UUID expected = UUID.randomUUID();

            when(this.factory.get()).thenReturn(expected);

            Key<UUID> result = generator.getKey();

            assertSame(expected, result.getId());
        }
    }

    @Test
    public void start() {
        this.subject.start();

        verify(this.affinity).start();
        verifyNoMoreInteractions(this.affinity);
    }

    @Test
    public void stop() {
        this.subject.stop();

        verify(this.affinity).stop();
        verifyNoMoreInteractions(this.affinity);
    }

    @Test
    public void createIdentifier() {
        UUID expected = UUID.randomUUID();

        when(this.affinity.getKeyForAddress(this.localAddress)).thenReturn(new GroupedKey<>(expected));

        UUID result = this.subject.get();

        assertSame(expected, result);
    }
}
