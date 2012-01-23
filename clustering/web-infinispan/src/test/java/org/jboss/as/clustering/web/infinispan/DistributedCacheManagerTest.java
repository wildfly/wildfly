/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.infinispan;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DataLocality;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.clustering.web.BatchingManager;
import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.jboss.as.clustering.web.IncomingDistributableSessionData;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.SessionOwnershipSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Paul Ferraro
 *
 */
public class DistributedCacheManagerTest {
    private LocalDistributableSessionManager sessionManager = mock(LocalDistributableSessionManager.class);
    @SuppressWarnings("unchecked")
    private SessionAttributeStorage<OutgoingDistributableSessionData> storage = mock(SessionAttributeStorage.class);
    @SuppressWarnings("unchecked")
    private AdvancedCache<String, Map<Object, Object>> cache = mock(AdvancedCache.class);
    @SuppressWarnings("unchecked")
    private Registry<String, Void> registry = mock(Registry.class);
    private SharedLocalYieldingClusterLockManager lockManager = mock(SharedLocalYieldingClusterLockManager.class);
    private BatchingManager batchingManager = mock(BatchingManager.class);
    private CacheInvoker invoker = mock(CacheInvoker.class);
    private DistributedCacheManager<OutgoingDistributableSessionData> manager;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(CacheMode.DIST_SYNC);

        when(this.cache.getCacheConfiguration()).thenReturn(builder.build());

        this.manager = new DistributedCacheManager<OutgoingDistributableSessionData>(this.sessionManager, this.cache, this.registry, this.lockManager, this.storage, this.batchingManager, this.invoker);

        reset(this.cache);
    }

    @After
    public void after() {
        reset(this.sessionManager, this.storage, this.cache, this.lockManager, this.batchingManager, this.invoker);
    }

    @Test
    public void start() {
        // Validate starting of cache per cache status
        this.start(ComponentStatus.FAILED, true);
        this.start(ComponentStatus.INITIALIZING, true);
        this.start(ComponentStatus.RUNNING, false);
        this.start(ComponentStatus.STOPPING, true);
        this.start(ComponentStatus.TERMINATED, true);
    }

    @SuppressWarnings("unchecked")
    private void start(ComponentStatus status, boolean startCache) {
        EmbeddedCacheManager container = mock(EmbeddedCacheManager.class);
        Address address = mock(Address.class);
        Cache<Address, String> jvmRouteCache = mock(Cache.class);
        String jvmRoute = "node0";
        
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getStatus()).thenReturn(status);
        when(this.cache.getCacheManager()).thenReturn(container);

        when(this.sessionManager.getJvmRoute()).thenReturn(jvmRoute);
        when(container.getAddress()).thenReturn(address);
        when(jvmRouteCache.putIfAbsent(same(address), same(jvmRoute))).thenReturn(null);

        this.manager.start();

        verify(this.cache).addListener(same(this.manager));

        reset(this.cache);
    }

    @Test
    public void stop() {
        EmbeddedCacheManager container = mock(EmbeddedCacheManager.class);

        when(this.cache.getCacheManager()).thenReturn(container);

        this.manager.stop();

        verify(this.cache).removeListener(same(this.manager));
    }

    @Test
    public void sessionCreated() {
        this.manager.sessionCreated("abc");

        verifyNoMoreInteractions(this.cache);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void storeSessionData() throws IOException {
        OutgoingDistributableSessionData data = mock(OutgoingDistributableSessionData.class);
        Map<Object, Object> map = mock(Map.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<CacheInvoker.Operation> capturedOperation = ArgumentCaptor.forClass(CacheInvoker.Operation.class);

        String sessionId = "abc";

        when(data.getRealId()).thenReturn(sessionId);
//        when(this.sessionCache.startBatch()).thenReturn(true);
        when(this.invoker.invoke(same(this.cache), capturedOperation.capture())).thenReturn(null);

        this.manager.storeSessionData(data);

//        verify(this.sessionCache).endBatch(true);

        CacheInvoker.Operation<String, Map<Object, Object>, Void> operation = capturedOperation.getValue();

        int version = 10;
        long timestamp = System.currentTimeMillis();
        DistributableSessionMetadata metadata = new DistributableSessionMetadata();

//        when(this.sessionCache.startBatch()).thenReturn(true);
        when(data.getVersion()).thenReturn(version);
        when(map.put(Byte.valueOf((byte) SessionMapEntry.VERSION.ordinal()), version)).thenReturn(null);
        when(data.getTimestamp()).thenReturn(timestamp);
        when(map.put(Byte.valueOf((byte) SessionMapEntry.TIMESTAMP.ordinal()), timestamp)).thenReturn(null);
        when(data.getMetadata()).thenReturn(metadata);
        when(map.put(Byte.valueOf((byte) SessionMapEntry.METADATA.ordinal()), metadata)).thenReturn(null);

        when(this.cache.putIfAbsent(eq(sessionId), Mockito.<Map<Object, Object>>anyObject())).thenReturn(map);

        operation.invoke(this.cache);

        verify(this.storage).store(same(map), same(data));
//        verify(this.sessionCache).endBatch(true);
    }

    @Test
    public void getSessionDataNoOwner() throws Exception {
        this.getSessionDataNoOwner(true);

        this.getSessionDataNoOwner(false);
    }

    @SuppressWarnings("unchecked")
    private void getSessionDataNoOwner(boolean includeAttributes) throws Exception {
        String sessionId = "abc";
        IncomingDistributableSessionData data = mock(IncomingDistributableSessionData.class);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<CacheInvoker.Operation> capturedOperation = ArgumentCaptor.forClass(CacheInvoker.Operation.class);

        when(this.invoker.invoke(same(this.cache), capturedOperation.capture())).thenReturn(data);

        IncomingDistributableSessionData result = this.manager.getSessionData(sessionId, null, includeAttributes);

        assertSame(data, result);

        Map<Object, Object> map = mock(Map.class);
        Map<String, Object> attributes = Collections.emptyMap();
        Integer version = Integer.valueOf(10);
        Long timestamp = Long.valueOf(System.currentTimeMillis());
        DistributableSessionMetadata metadata = new DistributableSessionMetadata();
        CacheInvoker.Operation<String, Map<Object, Object>, IncomingDistributableSessionData> operation = capturedOperation.getValue();

        when(this.cache.get(sessionId)).thenReturn(map);
        when(map.get(Byte.valueOf((byte) SessionMapEntry.VERSION.ordinal()))).thenReturn(version);
        when(map.get(Byte.valueOf((byte) SessionMapEntry.TIMESTAMP.ordinal()))).thenReturn(timestamp);
        when(map.get(Byte.valueOf((byte) SessionMapEntry.METADATA.ordinal()))).thenReturn(metadata);

        if (includeAttributes) {
            when(this.storage.load(map)).thenReturn(attributes);
        }

        result = operation.invoke(this.cache);

        assertNotNull(result);
        assertEquals(version.intValue(), result.getVersion());
        assertEquals(timestamp.longValue(), result.getTimestamp());
        assertSame(metadata, result.getMetadata());

        if (includeAttributes) {
            assertSame(attributes, result.getSessionAttributes());
        } else {
            IllegalStateException exception = null;
            Map<String, Object> sessionAttributes = null;

            try {
                sessionAttributes = result.getSessionAttributes();
            } catch (IllegalStateException e) {
                exception = e;
            }

            assertNull(sessionAttributes);
            assertNotNull(exception);
        }
    }

    @Test
    public void getMissingSessionDataNoOwner() {
        this.getMissingSessionDataNoOwner(true);

        this.getMissingSessionDataNoOwner(false);
    }

    @SuppressWarnings("unchecked")
    private void getMissingSessionDataNoOwner(boolean includeAttributes) {
        IncomingDistributableSessionData expected = mock(IncomingDistributableSessionData.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<CacheInvoker.Operation> capturedOperation = ArgumentCaptor.forClass(CacheInvoker.Operation.class);
        String sessionId = "abc";

        when(this.invoker.invoke(same(this.cache), capturedOperation.capture())).thenReturn(expected);

        IncomingDistributableSessionData result = this.manager.getSessionData(sessionId, null, includeAttributes);

        assertSame(expected, result);

        CacheInvoker.Operation<String, Map<Object, Object>, IncomingDistributableSessionData> operation = capturedOperation.getValue();

        when(this.cache.get(sessionId)).thenReturn(null);

        result = operation.invoke(this.cache);

        assertNull(result);
    }

    @Test
    public void getSessionDataWithOwner() {
        this.getSessionDataWithOwner(true);

        this.getSessionDataWithOwner(false);
    }

    private void getSessionDataWithOwner(boolean includeAttributes) {
        IncomingDistributableSessionData result = this.manager.getSessionData("abc", "owner1", includeAttributes);

        verifyZeroInteractions(this.cache);

        assertNull(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void removeSession() {
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<CacheInvoker.Operation> capturedOperation = ArgumentCaptor.forClass(CacheInvoker.Operation.class);

        when(this.invoker.invoke(same(this.cache), capturedOperation.capture())).thenReturn(null);

        this.manager.removeSession(sessionId);

        CacheInvoker.Operation<String, Map<Object, Object>, Void> operation = capturedOperation.getValue();

        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP)).thenReturn(this.cache);

        operation.invoke(this.cache);

        verify(this.cache).remove(sessionId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void removeSessionLocal() {
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<CacheInvoker.Operation> capturedOperation = ArgumentCaptor.forClass(CacheInvoker.Operation.class);

        when(this.invoker.invoke(same(this.cache), capturedOperation.capture())).thenReturn(null);

        this.manager.removeSessionLocal(sessionId);

        CacheInvoker.Operation<String, Map<Object, Object>, Void> operation = capturedOperation.getValue();

        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.withFlags(Flag.SKIP_CACHE_LOAD, Flag.CACHE_MODE_LOCAL)).thenReturn(this.cache);

        operation.invoke(this.cache);

        verify(this.cache).remove(sessionId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void removeSessionLocalNoOwner() {
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<CacheInvoker.Operation> capturedOperation = ArgumentCaptor.forClass(CacheInvoker.Operation.class);

        when(this.invoker.invoke(same(this.cache), capturedOperation.capture())).thenReturn(null);

        this.manager.removeSessionLocal(sessionId, null);

        CacheInvoker.Operation<String, Map<Object, Object>, Void> operation = capturedOperation.getValue();

        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.withFlags(Flag.SKIP_CACHE_LOAD, Flag.CACHE_MODE_LOCAL)).thenReturn(this.cache);

        operation.invoke(this.cache);

        verify(this.cache).remove(sessionId);
    }

    @Test
    public void removeSessionLocalWithOwner() {
        this.manager.removeSessionLocal("abc", "owner1");

        verifyZeroInteractions(this.cache);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void evictSession() {
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);

        when(this.invoker.invoke(same(this.cache), capturedOperation.capture())).thenReturn(null);

        this.manager.evictSession(sessionId);

        DistributedCacheManager<OutgoingDistributableSessionData>.Operation<Void> operation = capturedOperation.getValue();

        Void result = operation.invoke(this.cache);

        verify(this.cache).evict(sessionId);

        assertNull(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void evictSessionNoOwner() {
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);

        when(this.invoker.invoke(same(this.cache), capturedOperation.capture())).thenReturn(null);

        this.manager.evictSession(sessionId, null);

        DistributedCacheManager<OutgoingDistributableSessionData>.Operation<Void> operation = capturedOperation.getValue();

        Void result = operation.invoke(this.cache);

        verify(this.cache).evict(sessionId);

        assertNull(result);
    }

    @Test
    public void evictSessionLocalWithOwner() {
        this.manager.evictSession("abc", "owner1");

        verifyZeroInteractions(this.cache);
    }

    @Test
    public void getSessionIds() {
        String sessionId = "abc";

        when(this.cache.keySet()).thenReturn(Collections.singleton(sessionId));

        Map<String, String> result = this.manager.getSessionIds();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(sessionId));
        assertNull(result.get(sessionId));
    }

    @Test
    public void setForceSynchronous() {
        this.setForceSynchronous(true);
        this.setForceSynchronous(false);
    }

    @SuppressWarnings("unchecked")
    private void setForceSynchronous(boolean forceSynchronous) {
        this.manager.setForceSynchronous(forceSynchronous);
        
        AdvancedCache<String, Map<Object, Object>> syncCache = mock(AdvancedCache.class);
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);

        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.withFlags(Flag.FORCE_SYNCHRONOUS)).thenReturn(syncCache);
        when(this.invoker.invoke(same(forceSynchronous ? syncCache : this.cache), capturedOperation.capture())).thenReturn(null);

        this.manager.removeSession(sessionId);
    }

    @Test
    public void getSessionOwnershipSupport() {
        SessionOwnershipSupport support = this.manager.getSessionOwnershipSupport();

        assertSame(this.manager, support);
    }

    @Test
    public void removed() {
        @SuppressWarnings("unchecked")
        CacheEntryRemovedEvent<String, Map<Object, Object>> event = mock(CacheEntryRemovedEvent.class);

        when(event.isPre()).thenReturn(true);

        this.manager.removed(event);

        verifyZeroInteractions(this.sessionManager);

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(true);

        this.manager.removed(event);

        verifyNoMoreInteractions(this.sessionManager);

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(false);
        when(event.getCache()).thenReturn(this.cache);
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getKey()).thenReturn("abc");

        this.manager.removed(event);

        verify(this.sessionManager).notifyRemoteInvalidation("abc");
    }

    @Test
    public void modified() {
        @SuppressWarnings("unchecked")
        CacheEntryModifiedEvent<String, Map<Object, Object>> event = mock(CacheEntryModifiedEvent.class);

        when(event.isPre()).thenReturn(true);

        this.manager.modified(event);

        verifyZeroInteractions(this.sessionManager);

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(true);

        this.manager.modified(event);

        verifyZeroInteractions(this.sessionManager);

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(false);
        when(event.getCache()).thenReturn(this.cache);
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getKey()).thenReturn("abc");
        when(event.getValue()).thenReturn(Collections.emptyMap());

        this.manager.modified(event);

        verifyZeroInteractions(this.sessionManager);

        @SuppressWarnings("unchecked")
        Map<Object, Object> map = mock(Map.class);

        Integer version = Integer.valueOf(10);
        Long timestamp = Long.valueOf(System.currentTimeMillis());
        DistributableSessionMetadata metadata = new DistributableSessionMetadata();

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(false);
        when(event.getCache()).thenReturn(this.cache);
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getKey()).thenReturn("abc");
        when(event.getCache()).thenReturn(this.cache);
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getValue()).thenReturn(map);
        when(map.isEmpty()).thenReturn(false);

        when(map.get(Byte.valueOf((byte) SessionMapEntry.VERSION.ordinal()))).thenReturn(version);
        when(map.get(Byte.valueOf((byte) SessionMapEntry.TIMESTAMP.ordinal()))).thenReturn(timestamp);
        when(map.get(Byte.valueOf((byte) SessionMapEntry.METADATA.ordinal()))).thenReturn(metadata);

        this.manager.modified(event);

        verify(this.sessionManager).sessionChangedInDistributedCache("abc", null, version.intValue(), timestamp.longValue(), metadata);
    }

    @Test
    public void activated() {
        @SuppressWarnings("unchecked")
        CacheEntryActivatedEvent<String, Map<Object, Object>> event = mock(CacheEntryActivatedEvent.class);

        when(event.isPre()).thenReturn(true);

        this.manager.activated(event);

        verifyZeroInteractions(this.sessionManager);

        when(event.isPre()).thenReturn(false);
        when(event.getCache()).thenReturn(this.cache);
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());

        this.manager.activated(event);

        verify(this.sessionManager).sessionActivated();
    }

    @Test
    public void isLocal() {
        this.isLocal(null, true);
        this.isLocal(DataLocality.LOCAL, true);
        this.isLocal(DataLocality.LOCAL_UNCERTAIN, true);
        this.isLocal(DataLocality.NOT_LOCAL, false);
        this.isLocal(DataLocality.NOT_LOCAL_UNCERTAIN, true);
    }

    private void isLocal(DataLocality locality, boolean local) {
        DistributionManager distManager = mock(DistributionManager.class);
        String sessionId = "ABC123";

        when(this.cache.getAdvancedCache()).thenReturn(this.cache);

        if (locality != null) {
            when(this.cache.getDistributionManager()).thenReturn(distManager);
            when(distManager.getLocality(sessionId)).thenReturn(locality);
        } else {
            when(this.cache.getDistributionManager()).thenReturn(null);
        }

        boolean result = this.manager.isLocal(sessionId);

        assertEquals(local, result);
    }

    @Test
    public void locate() {
        String sessionId = "ABC123";
        String expected = "node1";

        // Test non-DIST
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getDistributionManager()).thenReturn(null);
        when(this.sessionManager.getJvmRoute()).thenReturn(expected);

        String result = this.manager.locate(sessionId);

        assertSame(expected, result);

        // Test rehash in progress
        DistributionManager distManager = mock(DistributionManager.class);

        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getDistributionManager()).thenReturn(distManager);
        when(distManager.getLocality(sessionId)).thenReturn(DataLocality.NOT_LOCAL_UNCERTAIN);
        when(this.sessionManager.getJvmRoute()).thenReturn(expected);

        result = this.manager.locate(sessionId);

        assertSame(expected, result);

        // Test session hashes locally
        EmbeddedCacheManager container = mock(EmbeddedCacheManager.class);
        Address address1 = mock(Address.class);
        Address address2 = mock(Address.class);
        Address localAddress = mock(Address.class);
        List<Address> addresses = Arrays.asList(address1, address2, localAddress);

        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getDistributionManager()).thenReturn(distManager);
        when(distManager.getLocality(sessionId)).thenReturn(DataLocality.LOCAL);
        when(distManager.locate(same(sessionId))).thenReturn(addresses);
        when(this.cache.getCacheManager()).thenReturn(container);
        when(container.getAddress()).thenReturn(address2);
        when(this.sessionManager.getJvmRoute()).thenReturn(expected);

        result = this.manager.locate(sessionId);

        assertSame(expected, result);

        // Test session does not hash locally
        addresses = Arrays.asList(address1, address2);
        ArgumentCaptor<Address> capturedAddress = ArgumentCaptor.forClass(Address.class);

        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.getDistributionManager()).thenReturn(distManager);
        when(distManager.getLocality(sessionId)).thenReturn(DataLocality.NOT_LOCAL);
        when(distManager.locate(same(sessionId))).thenReturn(addresses);
        when(this.cache.getCacheManager()).thenReturn(container);
        when(container.getAddress()).thenReturn(localAddress);
        when(this.registry.getRemoteEntry(capturedAddress.capture())).thenReturn(new AbstractMap.SimpleImmutableEntry<String, Void>(expected, null));
        when(this.cache.withFlags(Flag.FORCE_SYNCHRONOUS)).thenReturn(this.cache);

        result = this.manager.locate(sessionId);

        assertSame(expected, result);
        assertTrue(addresses.contains(capturedAddress.getValue()));
    }
}
