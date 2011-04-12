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

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.infinispan.CacheContainerRegistry;
import org.jboss.as.clustering.infinispan.DefaultCacheContainerRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Paul Ferraro
 */
public class CacheContainerRegistryService implements Service<CacheContainerRegistry> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("infinispan");

    private volatile String defaultContainer;
    private volatile CacheContainerRegistry registry;

    ServiceBuilder<CacheContainerRegistry> build(ServiceTarget target) {
        return target.addService(SERVICE_NAME, this).setInitialMode(ServiceController.Mode.ACTIVE);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public CacheContainerRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return this.registry;
    }

    public void setDefaultContainer(String defaultContainer) {
        this.defaultContainer = defaultContainer;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {
        DefaultCacheContainerRegistry registry = new DefaultCacheContainerRegistry();
        if (this.defaultContainer != null) {
            registry.setDefaultCacheContainerName(this.defaultContainer);
        }
        this.registry = registry;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.registry = null;
    }
}
