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
package org.wildfly.clustering.web.infinispan.sso;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.SimpleCacheInvoker;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOCacheEntry;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOFactory;

public class CoarseSSOFactoryTestCase {
    private final CacheInvoker invoker = new SimpleCacheInvoker();
    private final AdvancedCache<String, CoarseSSOCacheEntry<Object>> cache = mock(AdvancedCache.class);
    private final LocalContextFactory<Object> localContextFactory = mock(LocalContextFactory.class);
    
    private CoarseSSOFactory<Object> factory = new CoarseSSOFactory<>(this.cache, this.invoker, this.localContextFactory);

    @SuppressWarnings("rawtypes")
    @Test
    public void createValue() {
        ArgumentCaptor<CoarseSSOCacheEntry> capturedEntry = ArgumentCaptor.forClass(CoarseSSOCacheEntry.class);
        String newId = "new";
        String existingId = "existing";
        CoarseSSOCacheEntry<Object> entry = new CoarseSSOCacheEntry<>();
        
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.withFlags()).thenReturn(this.cache);
        when(this.cache.putIfAbsent(same(newId), capturedEntry.capture())).thenReturn(null);
        when(this.cache.putIfAbsent(same(existingId), any(CoarseSSOCacheEntry.class))).thenReturn(entry);
        
        CoarseSSOCacheEntry<Object> result = this.factory.createValue(newId);
        
        assertSame(capturedEntry.getValue(), result);

        result = this.factory.createValue(existingId);
        
        assertSame(entry, result);
    }

    @Test
    public void getValue() {
        String missingId = "missing";
        String existingId = "existing";
        CoarseSSOCacheEntry<Object> entry = new CoarseSSOCacheEntry<>();
        
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.withFlags()).thenReturn(this.cache);
        when(this.cache.get(missingId)).thenReturn(null);
        when(this.cache.get(existingId)).thenReturn(entry);
        
        CoarseSSOCacheEntry<Object> result = this.factory.findValue(missingId);
        
        assertNull(result);
        
        result = this.factory.findValue(existingId);
        
        assertSame(entry, result);
    }
    
    @Test
    public void remove() {
        String id = "id";
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.withFlags(Flag.IGNORE_RETURN_VALUES)).thenReturn(this.cache);
        
        this.factory.remove(id);
        
        verify(this.cache).remove(id);
    }
}
