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

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.factories.GlobalComponentRegistry;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

/**
 * @author Paul Ferraro
 */
public class GlobalComponentRegistryService implements Service<GlobalComponentRegistry> {

    public static final ServiceName getServiceName(String containerName) {
        return EmbeddedCacheManagerService.getServiceName(containerName).append("global-component-registry");
    }

    private final Value<CacheContainer> container;
    private volatile GlobalComponentRegistry registry;

    public GlobalComponentRegistryService(Value<CacheContainer> container) {
        this.container = container;
    }

    @Override
    public GlobalComponentRegistry getValue() {
        return this.registry;
    }

    @Override
    public void start(StartContext context) {
        this.registry = this.container.getValue().getGlobalComponentRegistry();
        this.registry.start();
    }

    @Override
    public void stop(StopContext context) {
        CacheContainer container = this.container.getValue();
        for (String cacheName: container.getCacheNames()) {
            if (container.isRunning(cacheName)) return;
        }
        this.registry.stop();
        this.registry = null;
    }
}
