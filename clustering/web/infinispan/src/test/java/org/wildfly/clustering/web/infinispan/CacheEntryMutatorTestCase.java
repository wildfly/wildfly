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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.jboss.as.clustering.infinispan.invoker.SimpleCacheInvoker;
import org.junit.Test;

/**
 * Unit test for {@link CacheEntryMutator}.
 * @author Paul Ferraro
 */
public class CacheEntryMutatorTestCase {

    @Test
    public void mutateWithBatching() {
        AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);
        Object id = new Object();
        Object value = new Object();
        Configuration config = new ConfigurationBuilder().invocationBatching().enable().build();

        when(cache.getCacheConfiguration()).thenReturn(config);

        Mutator mutator = new CacheEntryMutator<>(cache, new SimpleCacheInvoker(), id, value);
        
        when(cache.getAdvancedCache()).thenReturn(cache);
        when(cache.withFlags(Flag.IGNORE_RETURN_VALUES)).thenReturn(cache);
        
        mutator.mutate();
        
        verify(cache).replace(same(id), same(value));
        
        mutator.mutate();
        
        verify(cache, times(1)).replace(same(id), same(value));
        
        mutator.mutate();
        
        verify(cache, times(1)).replace(same(id), same(value));
    }

    @Test
    public void mutateWithOutBatching() {
        AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);
        Object id = new Object();
        Object value = new Object();
        Configuration config = new ConfigurationBuilder().invocationBatching().disable().build();

        when(cache.getCacheConfiguration()).thenReturn(config);

        Mutator mutator = new CacheEntryMutator<>(cache, new SimpleCacheInvoker(), id, value);
        
        when(cache.getAdvancedCache()).thenReturn(cache);
        when(cache.withFlags(Flag.IGNORE_RETURN_VALUES)).thenReturn(cache);
        
        mutator.mutate();
        
        verify(cache).replace(same(id), same(value));
        
        mutator.mutate();
        
        verify(cache, times(2)).replace(same(id), same(value));
        
        mutator.mutate();
        
        verify(cache, times(3)).replace(same(id), same(value));
    }
}
