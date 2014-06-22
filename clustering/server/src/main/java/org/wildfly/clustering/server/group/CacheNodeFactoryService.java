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
package org.wildfly.clustering.server.group;


import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.spi.ChannelServiceNames;

/**
 * {@link CacheNodeFactory} implementation that delegates node creation to the channel node factory
 * @author Paul Ferraro
 */
public class CacheNodeFactoryService implements Service<CacheNodeFactory> {

    public static ServiceBuilder<CacheNodeFactory> build(ServiceTarget target, ServiceName name, String containerName, String cacheName) {
        CacheNodeFactoryService service = new CacheNodeFactoryService();
        return target.addService(name, service)
                .addDependency(ChannelServiceNames.NODE_FACTORY.getServiceName(containerName), ChannelNodeFactory.class, service.factory)
        ;
    }

    private final InjectedValue<ChannelNodeFactory> factory = new InjectedValue<>();

    private volatile CacheNodeFactory value = null;

    private CacheNodeFactoryService() {
        // Hide
    }

    @Override
    public CacheNodeFactory getValue() {
        return this.value;
    }

    @Override
    public void start(StartContext context) {
        this.value = new CacheNodeFactoryImpl(this.factory.getValue());
    }

    @Override
    public void stop(StopContext context) {
        this.value = null;
    }
}
