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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DataLocality;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.web.BatchingManager;
import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.jboss.as.clustering.web.IncomingDistributableSessionData;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.SessionOwnershipSupport;
import org.jboss.msc.service.ServiceRegistry;
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
    private ServiceRegistry registry = mock(ServiceRegistry.class);
    @SuppressWarnings("unchecked")
    private SessionKeyFactory<SessionKey> keyFactory = mock(SessionKeyFactory.class);
    private LocalDistributableSessionManager sessionManager = mock(LocalDistributableSessionManager.class);
    @SuppressWarnings("unchecked")
    private SessionAttributeStorage<OutgoingDistributableSessionData> storage = mock(SessionAttributeStorage.class);
    @SuppressWarnings("unchecked")
    private AdvancedCache<SessionKey, Map<Object, Object>> sessionCache = mock(AdvancedCache.class);
    private CacheSource jvmRouteCacheSource = mock(CacheSource.class);
    private SharedLocalYieldingClusterLockManager lockManager = mock(SharedLocalYieldingClusterLockManager.class);
    private BatchingManager batchingManager = mock(BatchingManager.class);
    private CacheInvoker invoker = mock(CacheInvoker.class);
    private DistributedCacheManager<OutgoingDistributableSessionData, SessionKey> manager;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        Configuration configuration = new Configuration();
        configuration.fluent().mode(CacheMode.DIST_SYNC);

        when(this.sessionCache.getConfiguration()).thenReturn(configuration);

        this.manager = new DistributedCacheManager<OutgoingDistributableSessionData, SessionKey>(this.registry, this.sessionManager, this.sessionCache, this.jvmRouteCacheSource, this.lockManager, this.storage, this.batchingManager, this.keyFactory, this.invoker);

        reset(this.sessionCache);
    }

    @After
    public void after() {
        reset(this.registry, this.keyFactory, this.sessionManager, this.storage, this.sessionCache, this.jvmRouteCacheSource, this.lockManager, this.batchingManager, this.invoker);
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
    private DistributedCacheManager.JvmRouteHandler start(ComponentStatus status, boolean startCache) {
        EmbeddedCacheManager container = mock(EmbeddedCacheManager.class);
        Address address = mock(Address.class);
        Cache<Address, String> jvmRouteCache = mock(Cache.class);
        ArgumentCaptor<DistributedCacheManager.JvmRouteHandler> capturedJvmRouteHandler = ArgumentCaptor.forClass(DistributedCacheManager.JvmRouteHandler.class);
        String jvmRoute = "node0";
        
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getStatus()).thenReturn(status);
        when(this.sessionCache.getCacheManager()).thenReturn(container);

        when(this.sessionManager.getJvmRoute()).thenReturn(jvmRoute);
        when(container.getAddress()).thenReturn(address);
        when(this.jvmRouteCacheSource.<Address, String>getCache(this.registry, this.sessionManager)).thenReturn(jvmRouteCache);
        when(jvmRouteCache.putIfAbsent(same(address), same(jvmRoute))).thenReturn(null);

        this.manager.start();

        verify(container).addListener(capturedJvmRouteHandler.capture());
        verify(this.sessionCache).addListener(same(this.manager));

        reset(this.sessionCache);

        return capturedJvmRouteHandler.getValue();
    }

    @Test
    public void stop() {
        EmbeddedCacheManager container = mock(EmbeddedCacheManager.class);

        when(this.sessionCache.getCacheManager()).thenReturn(container);

        this.manager.stop();

        verify(container).removeListener(any(DistributedCacheManager.JvmRouteHandler.class));
        verify(this.sessionCache).removeListener(same(this.manager));
    }

    @Test
    public void sessionCreated() {
        this.manager.sessionCreated("abc");

        verifyNoMoreInteractions(this.sessionCache);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void storeSessionData() throws IOException {
        OutgoingDistributableSessionData data = mock(OutgoingDistributableSessionData.class);
        SessionKey key = mock(SessionKey.class);
        Map<Object, Object> map = mock(Map.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<CacheInvoker.Operation> capturedOperation = ArgumentCaptor.forClass(CacheInvoker.Operation.class);

        String sessionId = "abc";

        when(data.getRealId()).thenReturn(sessionId);
        when(this.keyFactory.createKey(sessionId)).thenReturn(key);
//        when(this.sessionCache.startBatch()).thenReturn(true);
        when(this.invoker.invoke(same(this.sessionCache), capturedOperation.capture())).thenReturn(null);

        this.manager.storeSessionData(data);

//        verify(this.sessionCache).endBatch(true);

        CacheInvoker.Operation<SessionKey, Map<Object, Object>, Void> operation = capturedOperation.getValue();

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

        when(this.sessionCache.putIfAbsent(same(key), Mockito.<Map<Object, Object>>anyObject())).thenReturn(map);

        operation.invoke(this.sessionCache);

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
        SessionKey key = mock(SessionKey.class);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);

        when(this.keyFactory.createKey(sessionId)).thenReturn(key);
        when(this.invoker.invoke(same(this.sessionCache), capturedOperation.capture())).thenReturn(data);

        IncomingDistributableSessionData result = this.manager.getSessionData(sessionId, null, includeAttributes);

        assertSame(data, result);

        Map<Object, Object> map = mock(Map.class);
        Map<String, Object> attributes = Collections.emptyMap();
        Integer version = Integer.valueOf(10);
        Long timestamp = Long.valueOf(System.currentTimeMillis());
        DistributableSessionMetadata metadata = new DistributableSessionMetadata();
        DistributedCacheManager<OutgoingDistributableSessionData, SessionKey>.Operation<IncomingDistributableSessionData> operation = capturedOperation.getValue();

        when(this.sessionCache.get(key)).thenReturn(map);
        when(map.get(Byte.valueOf((byte) SessionMapEntry.VERSION.ordinal()))).thenReturn(version);
        when(map.get(Byte.valueOf((byte) SessionMapEntry.TIMESTAMP.ordinal()))).thenReturn(timestamp);
        when(map.get(Byte.valueOf((byte) SessionMapEntry.METADATA.ordinal()))).thenReturn(metadata);

        if (includeAttributes) {
            when(this.storage.load(map)).thenReturn(attributes);
        }

        result = operation.invoke(this.sessionCache);

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
        SessionKey key = mock(SessionKey.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);
        String sessionId = "abc";

        when(this.keyFactory.createKey(sessionId)).thenReturn(key);
        when(this.invoker.invoke(same(this.sessionCache), capturedOperation.capture())).thenReturn(expected);

        IncomingDistributableSessionData result = this.manager.getSessionData(sessionId, null, includeAttributes);

        assertSame(expected, result);

        DistributedCacheManager<OutgoingDistributableSessionData, SessionKey>.Operation<IncomingDistributableSessionData> operation = capturedOperation.getValue();

        when(this.sessionCache.get(key)).thenReturn(null);

        result = operation.invoke(this.sessionCache);

        assertNull(result);
    }

    @Test
    public void getSessionDataWithOwner() {
        this.getSessionDataWithOwner(true);

        this.getSessionDataWithOwner(false);
    }

    private void getSessionDataWithOwner(boolean includeAttributes) {
        IncomingDistributableSessionData result = this.manager.getSessionData("abc", "owner1", includeAttributes);

        verifyZeroInteractions(this.sessionCache);

        assertNull(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void removeSession() {
        SessionKey key = mock(SessionKey.class);
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);

        when(this.keyFactory.createKey(sessionId)).thenReturn(key);
        when(this.invoker.invoke(same(this.sessionCache), capturedOperation.capture())).thenReturn(null);

        this.manager.removeSession(sessionId);

        DistributedCacheManager<OutgoingDistributableSessionData, SessionKey>.Operation<Map<Object, Object>> operation = capturedOperation.getValue();
        Map<Object, Object> expectedMap = mock(Map.class);

        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP)).thenReturn(this.sessionCache);
        when(this.sessionCache.remove(key)).thenReturn(expectedMap);

        Map<Object, Object> resultMap = operation.invoke(this.sessionCache);

        assertSame(expectedMap, resultMap);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void removeSessionLocal() {
        SessionKey key = mock(SessionKey.class);
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);

        when(this.keyFactory.createKey(sessionId)).thenReturn(key);
        when(this.invoker.invoke(same(this.sessionCache), capturedOperation.capture())).thenReturn(null);

        this.manager.removeSessionLocal(sessionId);

        DistributedCacheManager<OutgoingDistributableSessionData, SessionKey>.Operation<Map<Object, Object>> operation = capturedOperation.getValue();
        Map<Object, Object> expectedMap = mock(Map.class);

        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.withFlags(Flag.SKIP_CACHE_LOAD, Flag.CACHE_MODE_LOCAL)).thenReturn(this.sessionCache);
        when(this.sessionCache.remove(key)).thenReturn(expectedMap);

        Map<Object, Object> resultMap = operation.invoke(this.sessionCache);

        assertSame(expectedMap, resultMap);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void removeSessionLocalNoOwner() {
        SessionKey key = mock(SessionKey.class);
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);

        when(this.keyFactory.createKey(sessionId)).thenReturn(key);
        when(this.invoker.invoke(same(this.sessionCache), capturedOperation.capture())).thenReturn(null);

        this.manager.removeSessionLocal(sessionId, null);

        DistributedCacheManager<OutgoingDistributableSessionData, SessionKey>.Operation<Map<Object, Object>> operation = capturedOperation.getValue();
        Map<Object, Object> expectedMap = mock(Map.class);

        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.withFlags(Flag.SKIP_CACHE_LOAD, Flag.CACHE_MODE_LOCAL)).thenReturn(this.sessionCache);
        when(this.sessionCache.remove(key)).thenReturn(expectedMap);

        Map<Object, Object> resultMap = operation.invoke(this.sessionCache);

        assertSame(expectedMap, resultMap);
    }

    @Test
    public void removeSessionLocalWithOwner() {
        this.manager.removeSessionLocal("abc", "owner1");

        verifyZeroInteractions(this.sessionCache);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void evictSession() {
        SessionKey key = mock(SessionKey.class);
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);

        when(this.keyFactory.createKey(sessionId)).thenReturn(key);
        when(this.invoker.invoke(same(this.sessionCache), capturedOperation.capture())).thenReturn(null);

        this.manager.evictSession(sessionId);

        DistributedCacheManager<OutgoingDistributableSessionData, SessionKey>.Operation<Void> operation = capturedOperation.getValue();

        Void result = operation.invoke(this.sessionCache);

        verify(this.sessionCache).evict(key);

        assertNull(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void evictSessionNoOwner() {
        SessionKey key = mock(SessionKey.class);
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);

        when(this.keyFactory.createKey(sessionId)).thenReturn(key);
        when(this.invoker.invoke(same(this.sessionCache), capturedOperation.capture())).thenReturn(null);

        this.manager.evictSession(sessionId, null);

        DistributedCacheManager<OutgoingDistributableSessionData, SessionKey>.Operation<Void> operation = capturedOperation.getValue();

        Void result = operation.invoke(this.sessionCache);

        verify(this.sessionCache).evict(key);

        assertNull(result);
    }

    @Test
    public void evictSessionLocalWithOwner() {
        this.manager.evictSession("abc", "owner1");

        verifyZeroInteractions(this.sessionCache);
    }

    @Test
    public void getSessionIds() {
        SessionKey ourKey = mock(SessionKey.class);
        SessionKey foreignKey = mock(SessionKey.class);
        String sessionId = "abc";

        when(this.sessionCache.keySet()).thenReturn(new LinkedHashSet<SessionKey>(Arrays.asList(ourKey, foreignKey)));
        when(this.keyFactory.ours(ourKey)).thenReturn(true);
        when(ourKey.getSessionId()).thenReturn(sessionId);
        when(this.keyFactory.ours(foreignKey)).thenReturn(false);

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
        
        AdvancedCache<SessionKey, Map<Object, Object>> syncCache = mock(AdvancedCache.class);
        SessionKey key = mock(SessionKey.class);
        String sessionId = "abc";

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<DistributedCacheManager.Operation> capturedOperation = ArgumentCaptor.forClass(DistributedCacheManager.Operation.class);

        when(this.keyFactory.createKey(sessionId)).thenReturn(key);
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.withFlags(Flag.FORCE_SYNCHRONOUS)).thenReturn(syncCache);
        when(this.invoker.invoke(same(forceSynchronous ? syncCache : this.sessionCache), capturedOperation.capture())).thenReturn(null);

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
        CacheEntryRemovedEvent<SessionKey, Map<Object, Object>> event = mock(CacheEntryRemovedEvent.class);

        when(event.isPre()).thenReturn(true);

        this.manager.removed(event);

        verifyZeroInteractions(this.sessionManager);

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(true);

        this.manager.removed(event);

        verifyNoMoreInteractions(this.sessionManager);

        SessionKey key = mock(SessionKey.class);

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(false);
        when(event.getCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getKey()).thenReturn(key);
        when(this.keyFactory.ours(key)).thenReturn(false);

        verifyNoMoreInteractions(this.sessionManager);

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(false);
        when(event.getCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getKey()).thenReturn(key);
        when(this.keyFactory.ours(key)).thenReturn(true);
        when(key.getSessionId()).thenReturn("abc");

        this.manager.removed(event);

        verify(this.sessionManager).notifyRemoteInvalidation("abc");
    }

    @Test
    public void modified() {
        @SuppressWarnings("unchecked")
        CacheEntryModifiedEvent<SessionKey, Map<Object, Object>> event = mock(CacheEntryModifiedEvent.class);

        when(event.isPre()).thenReturn(true);

        this.manager.modified(event);

        verifyZeroInteractions(this.sessionManager);

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(true);

        this.manager.modified(event);

        verifyZeroInteractions(this.sessionManager);

        SessionKey key = mock(SessionKey.class);

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(false);
        when(event.getCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getKey()).thenReturn(key);
        when(this.keyFactory.ours(same(key))).thenReturn(false);

        this.manager.modified(event);

        verifyZeroInteractions(this.sessionManager);

        when(event.isPre()).thenReturn(false);
        when(event.isOriginLocal()).thenReturn(false);
        when(event.getCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getKey()).thenReturn(key);
        when(this.keyFactory.ours(same(key))).thenReturn(true);
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
        when(event.getCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getKey()).thenReturn(key);
        when(this.keyFactory.ours(same(key))).thenReturn(true);
        when(key.getSessionId()).thenReturn("abc");
        when(event.getCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
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
        CacheEntryActivatedEvent<SessionKey, Map<Object, Object>> event = mock(CacheEntryActivatedEvent.class);

        when(event.isPre()).thenReturn(true);

        this.manager.activated(event);

        verifyZeroInteractions(this.sessionManager);

        SessionKey key = mock(SessionKey.class);

        when(event.isPre()).thenReturn(false);
        when(event.getCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getKey()).thenReturn(key);
        when(this.keyFactory.ours(key)).thenReturn(false);

        verifyZeroInteractions(this.sessionManager);

        when(event.isPre()).thenReturn(false);
        when(event.getCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        when(event.getKey()).thenReturn(key);
        when(this.keyFactory.ours(key)).thenReturn(true);

        this.manager.activated(event);

        verify(this.sessionManager).sessionActivated();
    }

    @Test
    public void viewChanged() {
        ViewChangedEvent event = mock(ViewChangedEvent.class);
        Address newMember = mock(Address.class);
        Address member = mock(Address.class);
        Address oldMember = mock(Address.class);
        @SuppressWarnings("unchecked")
        Cache<Address, String> jvmRouteCache = mock(Cache.class);
        String jvmRoute = "node1";

        DistributedCacheManager.JvmRouteHandler handler = this.start(ComponentStatus.RUNNING, false);

        when(this.jvmRouteCacheSource.<Address, String>getCache(this.registry, this.sessionManager)).thenReturn(jvmRouteCache);

        when(jvmRouteCache.startBatch()).thenReturn(true);

        when(event.getOldMembers()).thenReturn(Arrays.asList(member, oldMember));
        when(event.getNewMembers()).thenReturn(Arrays.asList(member, newMember));

        when(event.getLocalAddress()).thenReturn(newMember);
        when(this.sessionManager.getJvmRoute()).thenReturn(jvmRoute);

        handler.viewChanged(event);

        verify(jvmRouteCache).remove(same(oldMember));
        verify(jvmRouteCache).put(same(newMember), same(jvmRoute));
        verify(jvmRouteCache).endBatch(true);
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

        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);

        if (locality != null) {
            when(this.sessionCache.getDistributionManager()).thenReturn(distManager);
            when(distManager.getLocality(sessionId)).thenReturn(locality);
        } else {
            when(this.sessionCache.getDistributionManager()).thenReturn(null);
        }

        boolean result = this.manager.isLocal(sessionId);

        assertEquals(local, result);
    }

    @Test
    public void locate() {
        String sessionId = "ABC123";
        String expected = "node1";

        // Test non-DIST
        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getDistributionManager()).thenReturn(null);
        when(this.sessionManager.getJvmRoute()).thenReturn(expected);

        String result = this.manager.locate(sessionId);

        assertSame(expected, result);

        // Test rehash in progress
        DistributionManager distManager = mock(DistributionManager.class);
        @SuppressWarnings("unchecked")
        Cache<Address, String> jvmRouteCache = mock(Cache.class);

        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getDistributionManager()).thenReturn(distManager);
        when(distManager.isRehashInProgress()).thenReturn(true);
        when(this.sessionManager.getJvmRoute()).thenReturn(expected);

        result = this.manager.locate(sessionId);

        assertSame(expected, result);

        // Test session hashes locally
        EmbeddedCacheManager container = mock(EmbeddedCacheManager.class);
        Address address1 = mock(Address.class);
        Address address2 = mock(Address.class);
        Address localAddress = mock(Address.class);
        List<Address> addresses = Arrays.asList(address1, address2, localAddress);

        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getDistributionManager()).thenReturn(distManager);
        when(distManager.isRehashInProgress()).thenReturn(false);
        when(distManager.locate(same(sessionId))).thenReturn(addresses);
        when(this.sessionCache.getCacheManager()).thenReturn(container);
        when(container.getAddress()).thenReturn(address2);
        when(this.sessionManager.getJvmRoute()).thenReturn(expected);

        result = this.manager.locate(sessionId);

        assertSame(expected, result);

        // Test session does not hash locally
        addresses = Arrays.asList(address1, address2);
        ArgumentCaptor<Address> capturedAddress = ArgumentCaptor.forClass(Address.class);

        when(this.sessionCache.getAdvancedCache()).thenReturn(this.sessionCache);
        when(this.sessionCache.getDistributionManager()).thenReturn(distManager);
        when(distManager.isRehashInProgress()).thenReturn(false);
        when(distManager.locate(same(sessionId))).thenReturn(addresses);
        when(this.sessionCache.getCacheManager()).thenReturn(container);
        when(container.getAddress()).thenReturn(localAddress);
        when(this.jvmRouteCacheSource.<Address, String>getCache(this.registry, this.sessionManager)).thenReturn(jvmRouteCache);
        when(jvmRouteCache.get(capturedAddress.capture())).thenReturn(expected);
        when(this.sessionCache.withFlags(Flag.FORCE_SYNCHRONOUS)).thenReturn(this.sessionCache);

        result = this.manager.locate(sessionId);

        assertSame(expected, result);
        assertTrue(addresses.contains(capturedAddress.getValue()));
    }
}
