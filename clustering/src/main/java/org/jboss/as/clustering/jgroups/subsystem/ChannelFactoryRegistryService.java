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
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.jgroups.ChannelFactoryRegistry;
import org.jboss.as.clustering.jgroups.DefaultChannelFactoryRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Provides the ChannelFactoryRegistry service.
 * @author Paul Ferraro
 */
public class ChannelFactoryRegistryService implements Service<ChannelFactoryRegistry> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jgroups");

    private volatile ChannelFactoryRegistry registry;
    private volatile String defaultStack;

    ServiceBuilder<ChannelFactoryRegistry> build(ServiceTarget target) {
        return target.addService(SERVICE_NAME, this).setInitialMode(ServiceController.Mode.ACTIVE);
    }

    public void setDefaultStack(String defaultStack) {
        this.defaultStack = defaultStack;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public ChannelFactoryRegistry getValue() {
        return this.registry;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {

        DefaultChannelFactoryRegistry registry = new DefaultChannelFactoryRegistry();

        if (this.defaultStack != null) {
            registry.setDefaultStack(this.defaultStack);
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
