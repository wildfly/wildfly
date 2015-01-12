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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.junit.After;
import org.junit.Test;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceNameFactory;

/**
 * @author Paul Ferraro
 */
public class DefaultCacheContainerTest {
    private final BatcherFactory batcherFactory = mock(BatcherFactory.class);
    private final EmbeddedCacheManager manager = mock(EmbeddedCacheManager.class);
    private final CacheContainer subject = new DefaultCacheContainer(this.manager, "default", this.batcherFactory);

    @After
    public void cleanup() {
        reset(this.manager);
    }

    @Test
    public void getDefaultCacheName() {
        assertEquals("default", this.subject.getDefaultCacheName());
    }

    @Test
    public void getDefaultCache() {
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
        AdvancedCache<Object, Object> defaultCache = mock(AdvancedCache.class);
        AdvancedCache<Object, Object> otherCache = mock(AdvancedCache.class);
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        TransactionBatch batch = mock(TransactionBatch.class);

        when(this.manager.<Object, Object>getCache("default", true)).thenReturn(defaultCache);
        when(this.manager.<Object, Object>getCache("other", true)).thenReturn(otherCache);
        when(defaultCache.getAdvancedCache()).thenReturn(defaultCache);
        when(otherCache.getAdvancedCache()).thenReturn(otherCache);
        when(this.batcherFactory.createBatcher(defaultCache)).thenReturn(null);
        when(this.batcherFactory.createBatcher(otherCache)).thenReturn(batcher);

        Cache<Object, Object> result = this.subject.getCache("default");
        
        assertNotSame(defaultCache, result);
        assertEquals(result, defaultCache);
        assertSame(this.subject, result.getCacheManager());

        // Validate no-op batching logic
        boolean started = result.startBatch();

        assertFalse(started);

        verify(defaultCache, never()).startBatch();

        result.endBatch(false);

        verify(defaultCache, never()).endBatch(false);

        when(this.batcherFactory.createBatcher(otherCache)).thenReturn(batcher);

        result = this.subject.getCache("other");

        assertNotSame(otherCache, result);
        assertEquals(result, otherCache);
        assertSame(this.subject, result.getCacheManager());

        // Validate batching logic
        when(batcher.createBatch()).thenReturn(batch);

        started = result.startBatch();

        assertTrue(started);

        started = result.startBatch();

        assertFalse(started);

        // Verify commit
        result.endBatch(true);

        verify(batch).close();
        reset(batch);

        // Verify re-commit is a no-op
        result.endBatch(true);

        verify(batch, never()).close();
        verify(batch, never()).discard();

        // Verify rollback
        started = result.startBatch();

        assertTrue(started);

        result.endBatch(false);

        verify(batch).discard();

        reset(batch);

        // Verify re-rollback is a no-op
        result.endBatch(true);

        verify(batch, never()).close();
        verify(batch, never()).discard();

        result = this.subject.getCache(CacheServiceNameFactory.DEFAULT_CACHE);

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
        ConfigurationBuilder builder = new ConfigurationBuilder();
        Configuration defaultConfig = builder.build();
        Configuration otherConfig = builder.build();
        
        when(this.manager.defineConfiguration("default", defaultConfig)).thenReturn(defaultConfig);
        when(this.manager.defineConfiguration("other", otherConfig)).thenReturn(otherConfig);
        
        Configuration result = this.subject.defineConfiguration("default", defaultConfig);
        
        assertSame(defaultConfig, result);
        
        result = this.subject.defineConfiguration("other", otherConfig);
        
        assertSame(otherConfig, result);
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
    public void getCacheManagerConfiguration() {
        GlobalConfiguration global = new GlobalConfigurationBuilder().build();
        
        when(this.manager.getCacheManagerConfiguration()).thenReturn(global);
        
        GlobalConfiguration result = this.subject.getCacheManagerConfiguration();
        
        assertSame(global, result);
    }

    @Test
    public void getDefaultCacheConfiguration() {
        Configuration config = new ConfigurationBuilder().build();
        
        when(this.manager.getDefaultCacheConfiguration()).thenReturn(config);
        
        Configuration result = this.subject.getDefaultCacheConfiguration();
        
        assertSame(config, result);
    }

    @Test
    public void getCacheConfiguration() {
        Configuration config = new ConfigurationBuilder().build();
        
        when(this.manager.getCacheConfiguration("cache")).thenReturn(config);
        
        Configuration result = this.subject.getCacheConfiguration("cache");
        
        assertSame(config, result);
    }

    @Test
    public void getCacheNames() {
        Set<String> caches = Collections.singleton("other");
        when(this.manager.getCacheNames()).thenReturn(caches);

        Set<String> result = this.subject.getCacheNames();

        assertEquals(1, result.size());
        assertTrue(result.contains("other"));
    }

    @Test
    public void isRunning() {
        when(this.manager.isRunning("other")).thenReturn(false);
        when(this.manager.isRunning("default")).thenReturn(true);

        boolean result = this.subject.isRunning("other");

        assertFalse(result);

        result = this.subject.isRunning("default");

        assertTrue(result);

        result = this.subject.isRunning(CacheServiceNameFactory.DEFAULT_CACHE);

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
        
        EmbeddedCacheManager result = this.subject.startCaches("other", CacheServiceNameFactory.DEFAULT_CACHE);
        
        assertSame(this.subject, result);
    }
}
