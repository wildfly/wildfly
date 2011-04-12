/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;

import org.infinispan.manager.CacheContainer;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class DefaultCacheContainerRegistryTest {
    private final DefaultCacheContainerRegistry registry = new DefaultCacheContainerRegistry();
    
    @Test
    public void getCacheContainerNames() {
        this.registry.addCacheContainer("test", Collections.singleton("alias"), mock(CacheContainer.class));
        
        Set<String> result = this.registry.getCacheContainerNames();
        
        assertEquals(Collections.singleton("test"), result);
        
        this.registry.removeCacheContainer("test");
        result = this.registry.getCacheContainerNames();
        
        assertEquals(Collections.emptySet(), result);
    }
    
    @Test
    public void getCacheContainer() {
        CacheContainer container = mock(CacheContainer.class);
        this.registry.addCacheContainer("test", Collections.singleton("alias"), container);
        
        CacheContainer result = this.registry.getCacheContainer("test");
        
        assertSame(container, result);
        
        result = this.registry.getCacheContainer("alias");
        
        assertSame(container, result);
        
        try {
            result = this.registry.getCacheContainer("unknown");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
    
    @Test
    public void addCacheContainer() {
        boolean result = this.registry.addCacheContainer("test", Collections.singleton("alias"), mock(CacheContainer.class));
        assertTrue(result);
        result = this.registry.addCacheContainer("test", Collections.singleton("alias"), mock(CacheContainer.class));
        assertFalse(result);
        result = this.registry.addCacheContainer("test2", Collections.singleton("alias"), mock(CacheContainer.class));
        assertTrue(result);
    }
    
    @Test
    public void removeCacheContainer() {
        boolean result = this.registry.removeCacheContainer("unknown");
        assertFalse(result);

        this.registry.addCacheContainer("test", Collections.singleton("alias"), mock(CacheContainer.class));
        result = this.registry.removeCacheContainer("test");
        assertTrue(result);
        result = this.registry.removeCacheContainer("test");
        assertFalse(result);
        
        this.registry.addCacheContainer("test", Collections.singleton("alias"), mock(CacheContainer.class));
        result = this.registry.removeCacheContainer("alias");
        assertTrue(result);
        result = this.registry.removeCacheContainer("alias");
        assertFalse(result);
    }
    
    @Test
    public void getDefaultCacheContainerName() {
        String result = this.registry.getDefaultCacheContainerName();
        
        assertNull(result);
        
        this.registry.addCacheContainer("test", Collections.singleton("alias"), mock(CacheContainer.class));
        
        result = this.registry.getDefaultCacheContainerName();
        
        assertEquals("test", result);
        
        this.registry.setDefaultCacheContainerName("default");
        
        result = this.registry.getDefaultCacheContainerName();
        
        assertEquals("default", result);
    }
}
