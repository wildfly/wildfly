/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.security.AllPermission;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.jboss.as.clustering.infinispan.invoker.SimpleCacheInvoker;
import org.junit.Test;
import org.wildfly.clustering.web.annotation.Immutable;
import org.wildfly.clustering.web.infinispan.session.CacheMutator;
import org.wildfly.clustering.web.sso.AuthenticationType;

public class CacheMutatorTestCase {

    @Test
    public void mutate() {
        AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);
        Object id = new Object();
        Object value = new Object();
        
        Mutator mutator = new CacheMutator<>(cache, new SimpleCacheInvoker(), id, value);
        
        when(cache.getAdvancedCache()).thenReturn(cache);
        when(cache.withFlags(Flag.IGNORE_RETURN_VALUES)).thenReturn(cache);
        
        mutator.mutate();
        
        verify(cache).replace(same(id), same(value));
        
        mutator.mutate();
        
        verify(cache, atMost(1)).replace(same(id), same(value));
    }

    @Test
    public void isMutable() throws MalformedURLException, UnknownHostException {
        assertTrue(CacheMutator.isMutable(new Object()));
        assertTrue(CacheMutator.isMutable(new Date()));
        assertTrue(CacheMutator.isMutable(new AtomicInteger()));
        assertTrue(CacheMutator.isMutable(new AtomicLong()));
        assertFalse(CacheMutator.isMutable(null));
        assertFalse(CacheMutator.isMutable(Collections.EMPTY_LIST));
        assertFalse(CacheMutator.isMutable(Collections.EMPTY_MAP));
        assertFalse(CacheMutator.isMutable(Collections.EMPTY_SET));
        assertFalse(CacheMutator.isMutable(Boolean.TRUE));
        assertFalse(CacheMutator.isMutable(Character.valueOf('a')));
        assertFalse(CacheMutator.isMutable(Currency.getInstance(Locale.US)));
        assertFalse(CacheMutator.isMutable(AuthenticationType.BASIC));
        assertFalse(CacheMutator.isMutable(Locale.getDefault()));
        assertFalse(CacheMutator.isMutable(Byte.valueOf(Integer.valueOf(1).byteValue())));
        assertFalse(CacheMutator.isMutable(Short.valueOf(Integer.valueOf(1).shortValue())));
        assertFalse(CacheMutator.isMutable(Integer.valueOf(1)));
        assertFalse(CacheMutator.isMutable(Long.valueOf(1)));
        assertFalse(CacheMutator.isMutable(Float.valueOf(1)));
        assertFalse(CacheMutator.isMutable(Double.valueOf(1)));
        assertFalse(CacheMutator.isMutable(BigInteger.valueOf(1)));
        assertFalse(CacheMutator.isMutable(BigDecimal.valueOf(1)));
        assertFalse(CacheMutator.isMutable(InetAddress.getLocalHost()));
        assertFalse(CacheMutator.isMutable(new InetSocketAddress(InetAddress.getLocalHost(), 80)));
        assertFalse(CacheMutator.isMutable(MathContext.UNLIMITED));
        assertFalse(CacheMutator.isMutable("test"));
        assertFalse(CacheMutator.isMutable(TimeZone.getDefault()));
        assertFalse(CacheMutator.isMutable(UUID.randomUUID()));
        File file = new File(System.getProperty("user.home"));
        assertFalse(CacheMutator.isMutable(file));
        assertFalse(CacheMutator.isMutable(file.toURI()));
        assertFalse(CacheMutator.isMutable(file.toURI().toURL()));
        assertFalse(CacheMutator.isMutable(FileSystems.getDefault().getRootDirectories().iterator().next()));
        assertFalse(CacheMutator.isMutable(new AllPermission()));
        assertFalse(CacheMutator.isMutable(new ImmutableObject()));
    }

    @Immutable
    static class ImmutableObject {
    }
}
