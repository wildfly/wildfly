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

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
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
public class JvmRouteCacheSourceTest {
    @SuppressWarnings("unchecked")
    @Test
    public void getCache() {
        ServiceRegistry registry = mock(ServiceRegistry.class);
        ServiceController<EmbeddedCacheManager> controller = mock(ServiceController.class);
        LocalDistributableSessionManager manager = mock(LocalDistributableSessionManager.class);
        Cache<Object, Object> cache = mock(Cache.class);
        EmbeddedCacheManager container = mock(EmbeddedCacheManager.class);
        ServiceNameProvider provider = mock(ServiceNameProvider.class);
        ReplicationConfig config = new ReplicationConfig();
        config.setCacheName("test-cache");

        String engine = "engine";
        ServiceName name = ServiceName.JBOSS.append("infinispan");

        CacheSource source = new JvmRouteCacheSource(provider);

        when(manager.getReplicationConfig()).thenReturn(config);
        when(provider.getServiceName(same(config))).thenReturn(name);
        when((ServiceController<EmbeddedCacheManager>) registry.getRequiredService(name)).thenReturn(controller);
        when(controller.getValue()).thenReturn(container);
        when(manager.getEngineName()).thenReturn(engine);
        when(container.getCache(engine)).thenReturn(cache);

        assertSame(cache, source.getCache(registry, manager));
    }
}
