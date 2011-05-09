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

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class SessionCacheSourceTest {
    @SuppressWarnings("unchecked")
    @Test
    public void getCache() {
        ReplicationConfig config = new ReplicationConfig();
        config.setCacheName("test");

        ServiceRegistry registry = mock(ServiceRegistry.class);
        ServiceController<Cache<Object, Object>> controller = mock(ServiceController.class);
        LocalDistributableSessionManager manager = mock(LocalDistributableSessionManager.class);
        Cache<Object, Object> cache = mock(Cache.class);
        ServiceNameProvider provider = mock(ServiceNameProvider.class);
        ServiceName name = ServiceName.JBOSS.append("infinispan", "web", CacheContainer.DEFAULT_CACHE_NAME);

        CacheSource source = new SessionCacheSource(provider);

        when(manager.getReplicationConfig()).thenReturn(config);
        when(provider.getServiceName(same(config))).thenReturn(name);
        when((ServiceController<Cache<Object, Object>>) registry.getRequiredService(name)).thenReturn(controller);
        when(controller.getValue()).thenReturn(cache);

        assertSame(cache, source.getCache(registry, manager));
    }
/*
    @Test
    public void getCache() {
        Configuration configuration = new Configuration();
        configuration.fluent().mode(CacheMode.REPL_ASYNC);
        ReplicationConfig config = new ReplicationConfig();
        config.setCacheName("standard-session-cache");
        config.setReplicationMode(null);
        config.setBackups(null);

        this.getCache("standard-session-cache", null, "//host/context1", config, configuration, CacheMode.REPL_ASYNC, null);
        this.getCache("standard-session-cache", null, "//host/context2", config, configuration, CacheMode.REPL_ASYNC, null);

        // Validate cache container qualified cache name
        config.setCacheName("default/session-cache");
        this.getCache("default", "session-cache", "//host/context", config, configuration, CacheMode.REPL_ASYNC, null);

        config.setCacheName(null);
        this.getCache(null, null, "//host/context", config, configuration, CacheMode.REPL_ASYNC, null);

        config.setCacheName("standard-session-cache");
        config.setReplicationMode(ReplicationMode.SYNCHRONOUS);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.REPL_SYNC, null);

        config.setReplicationMode(ReplicationMode.ASYNCHRONOUS);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.REPL_ASYNC, null);

        config.setBackups(Integer.valueOf(-1));
        config.setReplicationMode(null);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.REPL_ASYNC, true);

        config.setReplicationMode(ReplicationMode.SYNCHRONOUS);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.REPL_SYNC, true);

        config.setReplicationMode(ReplicationMode.ASYNCHRONOUS);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.REPL_ASYNC, true);

        config.setBackups(Integer.valueOf(0));
        config.setReplicationMode(null);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.LOCAL, null);

        config.setReplicationMode(ReplicationMode.SYNCHRONOUS);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.LOCAL, null);

        config.setReplicationMode(ReplicationMode.ASYNCHRONOUS);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.LOCAL, null);

        config.setBackups(Integer.valueOf(1));
        config.setReplicationMode(null);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.DIST_ASYNC, false);

        config.setReplicationMode(ReplicationMode.SYNCHRONOUS);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.DIST_SYNC, false);

        config.setReplicationMode(ReplicationMode.ASYNCHRONOUS);
        this.getCache("standard-session-cache", null, "//host/context", config, configuration, CacheMode.DIST_ASYNC, false);
    }

    @SuppressWarnings("unchecked")
    private void getCache(String containerName, String templateCacheName, String managerName, ReplicationConfig config, Configuration configuration, CacheMode mode, Boolean fetchInMemoryState) {
        ServiceRegistry registry = mock(ServiceRegistry.class);
        ServiceController<EmbeddedCacheManager> controller = mock(ServiceController.class);
        LocalDistributableSessionManager manager = mock(LocalDistributableSessionManager.class);
        Cache<Object, Object> cache = mock(Cache.class);
        EmbeddedCacheManager container = mock(EmbeddedCacheManager.class);

        when((ServiceController<EmbeddedCacheManager>) registry.getRequiredService(eq(EmbeddedCacheManagerService.getServiceName("web")))).thenReturn(controller);
        when(controller.getValue()).thenReturn(container);
        when(manager.getReplicationConfig()).thenReturn(config);
        when(container.getCache(config.getCacheName())).thenReturn(cache);

        assertSame(cache, this.source.getCache(registry, manager));
    }
*/
}
