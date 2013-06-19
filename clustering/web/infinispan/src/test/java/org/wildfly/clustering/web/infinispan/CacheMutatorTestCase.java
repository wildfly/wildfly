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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.SimpleCacheInvoker;
import org.junit.Test;
import org.wildfly.clustering.annotation.Immutable;
import org.wildfly.clustering.web.sso.AuthenticationType;

public class CacheMutatorTestCase {

    @Test
    public void mutate() {
        @SuppressWarnings("unchecked")
        AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);
        Object id = new Object();
        Object value = new Object();
        
        Mutator mutator = new CacheMutator<Object, Object>(cache, new SimpleCacheInvoker(), id, value);
        
        when(cache.getAdvancedCache()).thenReturn(cache);
        when(cache.withFlags(Flag.IGNORE_RETURN_VALUES)).thenReturn(cache);
        
        mutator.mutate();
        
        verify(cache).replace(same(id), same(value));
        
        mutator.mutate();
        
        verify(cache, atMost(1)).replace(same(id), same(value));
    }

    @Test
    public void isMutable() {
        assertTrue(CacheMutator.isMutable(new Object()));
        assertTrue(CacheMutator.isMutable(new Date()));
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
        assertFalse(CacheMutator.isMutable("test"));
        assertFalse(CacheMutator.isMutable(TimeZone.getDefault()));
        assertFalse(CacheMutator.isMutable(UUID.randomUUID()));
        assertFalse(CacheMutator.isMutable(new ImmutableObject()));
    }

    @Immutable
    static class ImmutableObject {
    }
}
