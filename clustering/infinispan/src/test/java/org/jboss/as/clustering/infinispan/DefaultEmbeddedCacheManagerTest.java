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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.ConfigurationException;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.junit.After;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class DefaultEmbeddedCacheManagerTest {
    private final EmbeddedCacheManager manager = mock(EmbeddedCacheManager.class);
    private final EmbeddedCacheManager subject = new DefaultEmbeddedCacheManager(this.manager, "default");
    
    @After
    public void cleanup() {
        reset(manager);
    }
    
    @Test
    public void getDefaultCache() {
        @SuppressWarnings("unchecked")
        AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);

        when(this.manager.<Object, Object>getCache("default", true)).thenReturn(cache);
        when(cache.getAdvancedCache()).thenReturn(cache);
        
        Cache<Object, Object> result = this.subject.getCache();

        assertNotSame(cache, result);
        assertEquals(result, cache);
        assertSame(this.subject, result.getCacheManager());
    }

    @Test
    public void getCache() {
        @SuppressWarnings("unchecked")
        AdvancedCache<Object, Object> defaultCache = mock(AdvancedCache.class);
        @SuppressWarnings("unchecked")
        AdvancedCache<Object, Object> otherCache = mock(AdvancedCache.class);
        
        when(this.manager.<Object, Object>getCache("default", true)).thenReturn(defaultCache);
        when(this.manager.<Object, Object>getCache("other", true)).thenReturn(otherCache);
        when(defaultCache.getAdvancedCache()).thenReturn(defaultCache);
        when(otherCache.getAdvancedCache()).thenReturn(otherCache);
        
        Cache<Object, Object> result = this.subject.getCache("default");
        
        assertNotSame(defaultCache, result);
        assertEquals(result, defaultCache);
        assertSame(this.subject, result.getCacheManager());

        result = this.subject.getCache("other");

        assertNotSame(otherCache, result);
        assertEquals(result, otherCache);
        assertSame(this.subject, result.getCacheManager());

        result = this.subject.getCache(CacheContainer.DEFAULT_CACHE_NAME);

        assertNotSame(defaultCache, result);
        assertEquals(result, defaultCache);
        assertSame(this.subject, result.getCacheManager());

        result = this.subject.getCache(null);

        assertNotSame(defaultCache, result);
        assertEquals(result, defaultCache);
        assertSame(this.subject, result.getCacheManager());
    }

    @Test
    public void start() {
        this.subject.start();

        verify(this.manager).start();
    }

    @Test
    public void stop() {
        this.subject.stop();

        verify(this.manager).stop();
    }

    @Test
    public void addListener() {
        Object listener = new Object();
        this.subject.addListener(listener);

        verify(this.manager).addListener(listener);
    }

    @Test
    public void removeListener() {
        Object listener = new Object();
        this.subject.removeListener(listener);

        verify(this.manager).removeListener(listener);
    }

    @Test
    public void getListeners() {
        Set<Object> expected = Collections.singleton(new Object());
        when(this.manager.getListeners()).thenReturn(expected);

        Set<Object> result = this.subject.getListeners();

        assertSame(expected, result);
    }

    @Test
    public void defineConfiguration() {
        Configuration defaultConfig = new Configuration();
        Configuration defaultConfigOverride = new Configuration();
        Configuration otherConfig = new Configuration();
        Configuration otherConfigOverride = new Configuration();

        when(this.manager.defineConfiguration("default", defaultConfigOverride)).thenReturn(defaultConfig);
        when(this.manager.defineConfiguration("other", otherConfigOverride)).thenReturn(otherConfig);

        Configuration result = this.subject.defineConfiguration("default", defaultConfigOverride);

        assertSame(defaultConfig, result);

        result = this.subject.defineConfiguration("other", otherConfigOverride);

        assertSame(otherConfig, result);

        result = this.subject.defineConfiguration(CacheContainer.DEFAULT_CACHE_NAME, defaultConfigOverride);

        assertSame(defaultConfig, result);

        result = this.subject.defineConfiguration(null, defaultConfigOverride);

        assertSame(defaultConfig, result);
    }

    @Test
    public void defineConfigurationWithTemplate() {
        Configuration defaultConfig = new Configuration();
        Configuration defaultConfigOverride = new Configuration();
        Configuration otherConfig = new Configuration();
        Configuration otherConfigOverride = new Configuration();

        when(this.manager.defineConfiguration("default", "template", defaultConfigOverride)).thenReturn(defaultConfigOverride);
        when(this.manager.defineConfiguration("default", "default", defaultConfigOverride)).thenReturn(defaultConfig);
        when(this.manager.defineConfiguration("other", "template", otherConfigOverride)).thenReturn(otherConfigOverride);
        when(this.manager.defineConfiguration("other", "default", otherConfigOverride)).thenReturn(otherConfig);

        Configuration result = this.subject.defineConfiguration("default", "template", defaultConfigOverride);

        assertSame(defaultConfigOverride, result);

        result = this.subject.defineConfiguration("default", "default", defaultConfigOverride);

        assertSame(defaultConfig, result);

        result = this.subject.defineConfiguration("default", CacheContainer.DEFAULT_CACHE_NAME, defaultConfigOverride);

        assertSame(defaultConfig, result);

        result = this.subject.defineConfiguration("default", null, defaultConfigOverride);

        assertSame(defaultConfig, result);

        result = this.subject.defineConfiguration("other", "template", otherConfigOverride);

        assertSame(otherConfigOverride, result);

        result = this.subject.defineConfiguration("other", "default", otherConfigOverride);

        assertSame(otherConfig, result);

        result = this.subject.defineConfiguration("other", CacheContainer.DEFAULT_CACHE_NAME, otherConfigOverride);

        assertSame(otherConfig, result);

        result = this.subject.defineConfiguration("other", null, otherConfigOverride);

        assertSame(otherConfig, result);

        result = this.subject.defineConfiguration(CacheContainer.DEFAULT_CACHE_NAME, "template", defaultConfigOverride);

        assertSame(defaultConfigOverride, result);

        result = this.subject.defineConfiguration(CacheContainer.DEFAULT_CACHE_NAME, "default", defaultConfigOverride);

        assertSame(defaultConfig, result);

        result = this.subject.defineConfiguration(CacheContainer.DEFAULT_CACHE_NAME, CacheContainer.DEFAULT_CACHE_NAME, defaultConfigOverride);

        assertSame(defaultConfig, result);

        result = this.subject.defineConfiguration(CacheContainer.DEFAULT_CACHE_NAME, null, defaultConfigOverride);

        assertSame(defaultConfig, result);

        result = this.subject.defineConfiguration(null, "template", defaultConfigOverride);

        assertSame(defaultConfigOverride, result);

        result = this.subject.defineConfiguration(null, "default", defaultConfigOverride);

        assertSame(defaultConfig, result);

        result = this.subject.defineConfiguration(null, CacheContainer.DEFAULT_CACHE_NAME, defaultConfigOverride);

        assertSame(defaultConfig, result);

        result = this.subject.defineConfiguration(null, null, defaultConfigOverride);

        assertSame(defaultConfig, result);
    }

    @Test
    public void getClusterName() {
        String expected = "cluster";
        when(this.manager.getClusterName()).thenReturn(expected);

        String result = this.subject.getClusterName();

        assertSame(expected, result);
    }

    @Test
    public void getMembers() {
        List<Address> expected = Collections.singletonList(mock(Address.class));
        when(this.manager.getMembers()).thenReturn(expected);

        List<Address> result = this.subject.getMembers();

        assertSame(expected, result);
    }

    @Test
    public void getAddress() {
        Address expected = mock(Address.class);
        when(this.manager.getAddress()).thenReturn(expected);

        Address result = this.subject.getAddress();

        assertSame(expected, result);
    }

    @Test
    public void getCoordinator() {
        Address expected = mock(Address.class);
        when(this.manager.getCoordinator()).thenReturn(expected);

        Address result = this.subject.getCoordinator();

        assertSame(expected, result);
    }

    @Test
    public void getStatus() {
        ComponentStatus expected = ComponentStatus.INITIALIZING;
        when(this.manager.getStatus()).thenReturn(expected);

        ComponentStatus result = this.subject.getStatus();

        assertSame(expected, result);
    }

    @Test
    public void getGlobalConfiguration() {
        GlobalConfiguration expected = new GlobalConfiguration();
        when(this.manager.getGlobalConfiguration()).thenReturn(expected);

        GlobalConfiguration result = this.subject.getGlobalConfiguration();

        assertSame(expected, result);
    }

    @Test
    public void getDefaultConfiguration() {
        Configuration expected = new Configuration();
        when(this.manager.getDefaultConfiguration()).thenReturn(expected);

        Configuration result = this.subject.getDefaultConfiguration();

        assertSame(expected, result);
    }

    @Test
    public void getCacheNames() {
        Set<String> caches = Collections.singleton("other");
        when(this.manager.getCacheNames()).thenReturn(caches);

        Set<String> result = this.subject.getCacheNames();

        assertEquals(2, result.size());
        assertTrue(result.contains("other"));
        assertTrue(result.contains("default"));
    }

    @Test
    public void isRunning() {
        when(this.manager.isRunning("other")).thenReturn(false);
        when(this.manager.isRunning("default")).thenReturn(true);

        boolean result = this.subject.isRunning("other");

        assertFalse(result);

        result = this.subject.isRunning("default");

        assertTrue(result);

        result = this.subject.isRunning(CacheContainer.DEFAULT_CACHE_NAME);

        assertTrue(result);

        result = this.subject.isRunning(null);

        assertTrue(result);
    }

    @Test
    public void isDefaultRunning() {
        when(this.manager.isRunning("default")).thenReturn(true);

        boolean result = this.subject.isDefaultRunning();

        assertTrue(result);
    }
    
    @Test
    public void startCaches() {
        when(this.manager.startCaches("other", "default")).thenReturn(this.manager);
        
        EmbeddedCacheManager result = this.subject.startCaches("other", CacheContainer.DEFAULT_CACHE_NAME);
        
        assertSame(this.subject, result);
    }
}
