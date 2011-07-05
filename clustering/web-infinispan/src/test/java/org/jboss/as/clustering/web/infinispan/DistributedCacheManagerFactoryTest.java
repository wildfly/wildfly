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

import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.infinispan.AdvancedCache;
import org.infinispan.config.Configuration;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.jboss.as.clustering.web.SessionAttributeMarshallerFactory;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class DistributedCacheManagerFactoryTest {
    private ServiceRegistry registry = mock(ServiceRegistry.class);
    private CacheSource sessionCacheSource = mock(CacheSource.class);
    private CacheSource jvmRouteCacheSource = mock(CacheSource.class);
    private LockManagerSource lockManagerSource = mock(LockManagerSource.class);
    private SessionAttributeStorageFactory storageFactory = mock(SessionAttributeStorageFactory.class);
    private SessionAttributeMarshallerFactory marshallerFactory = mock(SessionAttributeMarshallerFactory.class);
    private CacheInvoker invoker = mock(CacheInvoker.class);
    private LocalDistributableSessionManager manager = mock(LocalDistributableSessionManager.class);
    @SuppressWarnings("unchecked")
    private SessionAttributeStorage<OutgoingDistributableSessionData> storage = mock(SessionAttributeStorage.class);
    private SessionAttributeMarshaller marshaller = mock(SessionAttributeMarshaller.class);
    @SuppressWarnings("unchecked")
    private AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);
    private DistributedCacheManagerFactory factory;

    @Before
    public void before() {
        this.factory = new DistributedCacheManagerFactory();
        this.factory.setSessionAttributeStorageFactory(this.storageFactory);
        this.factory.setSessionAttributeMarshallerFactory(this.marshallerFactory);
        this.factory.setCacheInvoker(this.invoker);
        this.factory.setSessionCacheSource(this.sessionCacheSource);
        this.factory.setJvmRouteCacheSource(this.jvmRouteCacheSource);
        this.factory.setLockManagerSource(this.lockManagerSource);
    }

    @After
    public void after() {
        reset(this.registry, this.sessionCacheSource, this.jvmRouteCacheSource, this.lockManagerSource, this.storageFactory, this.marshallerFactory, this.invoker, this.manager, this.storage, this.marshaller, this.cache);
    }

    @Test
    public void getDistributedCacheManager() throws Exception {
        ReplicationConfig config = new ReplicationConfig();
        ReplicationGranularity granularity = ReplicationGranularity.SESSION;
        config.setReplicationGranularity(granularity);
        Configuration configuration = new Configuration();
        configuration.fluent().invocationBatching();

        when(this.sessionCacheSource.getCache(same(this.registry), same(this.manager))).thenReturn(this.cache);
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.with(same(DistributedCacheManagerFactory.class.getClassLoader()))).thenReturn(this.cache);
        when(this.cache.getConfiguration()).thenReturn(configuration);
        when(this.lockManagerSource.getLockManager(same(this.cache))).thenReturn(null);
        when(this.cache.getTransactionManager()).thenReturn(new BatchModeTransactionManager());

        when(this.marshallerFactory.createMarshaller(this.manager)).thenReturn(this.marshaller);
        when(this.manager.getReplicationConfig()).thenReturn(config);
        when(this.storageFactory.createStorage(granularity, this.marshaller)).thenReturn(this.storage);

        when(this.cache.getAdvancedCache()).thenReturn(this.cache);

        org.jboss.as.clustering.web.DistributedCacheManager<?> result = this.factory.getDistributedCacheManager(this.registry, this.manager);

        assertNotNull(result);
        assertTrue(result instanceof DistributedCacheManager);
    }

    @Test
    public void addDependencies() {
        this.verifyDependencies(null, CacheService.getServiceName(DistributedCacheManagerFactory.DEFAULT_CACHE_CONTAINER, null), EmbeddedCacheManagerService.getServiceName(null));
        this.verifyDependencies("web", CacheService.getServiceName("web", null), EmbeddedCacheManagerService.getServiceName(null));
        this.verifyDependencies("web.dist", CacheService.getServiceName("web", "dist"), EmbeddedCacheManagerService.getServiceName(null));
        this.verifyDependencies("alias", CacheService.getServiceName("alias", null), EmbeddedCacheManagerService.getServiceName(null));
        this.verifyDependencies("alias.dist", CacheService.getServiceName("alias", "dist"), EmbeddedCacheManagerService.getServiceName(null));
        this.verifyDependencies("jboss.infinispan.web", CacheService.getServiceName("web", null), EmbeddedCacheManagerService.getServiceName(null));
        this.verifyDependencies("jboss.infinispan.web.dist", CacheService.getServiceName("web", "dist"), EmbeddedCacheManagerService.getServiceName(null));
        this.verifyDependencies("jboss.infinispan.alias", CacheService.getServiceName("alias", null), EmbeddedCacheManagerService.getServiceName(null));
        this.verifyDependencies("jboss.infinispan.alias.dist", CacheService.getServiceName("alias", "dist"), EmbeddedCacheManagerService.getServiceName(null));
    }

    private void verifyDependencies(String cacheName, ServiceName sessionCacheServiceName, ServiceName jvmRouteCacheServiceName) {
        ReplicationConfig config = new ReplicationConfig();
        config.setCacheName(cacheName);
        JBossWebMetaData metaData = new JBossWebMetaData();
        metaData.setReplicationConfig(config);

        Collection<ServiceName> dependencies = this.factory.getDependencies(metaData);

        assertEquals(2, dependencies.size());
        assertTrue(dependencies.contains(sessionCacheServiceName));
        assertTrue(dependencies.contains(jvmRouteCacheServiceName));
    }

    @Test
    public void load() {
        Iterator<org.jboss.as.clustering.web.DistributedCacheManagerFactory> factories = ServiceLoader.load(org.jboss.as.clustering.web.DistributedCacheManagerFactory.class).iterator();

        assertTrue(factories.hasNext());

        org.jboss.as.clustering.web.DistributedCacheManagerFactory factory = factories.next();

        assertNotNull(factory);
        assertTrue(factory instanceof DistributedCacheManagerFactory);

        assertFalse(factories.hasNext());
    }
}
