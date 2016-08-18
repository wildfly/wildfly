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

package org.wildfly.clustering.web.infinispan;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.web.IdentifierFactory;

/**
 * Unit test for {@link AffinityIdentifierFactory}
 *
 * @author Paul Ferraro
 */
public class AffinityIdentityFactoryTestCase {

    private final IdentifierFactory<String> factory = mock(IdentifierFactory.class);
    private final KeyAffinityServiceFactory affinityFactory = mock(KeyAffinityServiceFactory.class);
    private final KeyAffinityService<Key<String>> affinity = mock(KeyAffinityService.class);
    private final Cache<Key<String>, ?> cache = mock(Cache.class);
    private final EmbeddedCacheManager manager = mock(EmbeddedCacheManager.class);

    private IdentifierFactory<String> subject;

    @Captor
    private ArgumentCaptor<KeyGenerator<Key<String>>> capturedGenerator;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        when(this.affinityFactory.createService(same(this.cache), this.capturedGenerator.capture())).thenReturn(this.affinity);
        when(this.cache.getCacheManager()).thenReturn(this.manager);

        this.subject = new AffinityIdentifierFactory<>(this.factory, this.cache, this.affinityFactory);

        KeyGenerator<Key<String>> generator = this.capturedGenerator.getValue();

        assertSame(generator, this.subject);

        String expected = "id";

        when(this.factory.createIdentifier()).thenReturn(expected);

        Key<String> result = generator.getKey();

        assertSame(expected, result.getValue());
    }

    @Test
    public void createIdentifier() {
        String expected = "id";
        Address address = mock(Address.class);

        when(this.manager.getAddress()).thenReturn(address);
        when(this.affinity.getKeyForAddress(address)).thenReturn(new Key<>(expected));

        String result = this.subject.createIdentifier();

        assertSame(expected, result);
    }
}
