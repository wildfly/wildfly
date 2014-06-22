/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.remote;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.CacheServiceNames;

public class RegistryInstallerService implements Service<Void> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "remoting", "connector", "client-mappings", "installer");

    @SuppressWarnings("rawtypes")
    private final InjectedValue<RegistryCollector> collector = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Registry> registry = new InjectedValue<>();

    public ServiceBuilder<Void> build(ServiceTarget target) {
        return target.addService(SERVICE_NAME, this)
                .addDependency(RegistryCollectorService.SERVICE_NAME, RegistryCollector.class, this.collector)
                .addDependency(CacheServiceNames.REGISTRY.getServiceName(BeanManagerFactoryBuilderConfiguration.DEFAULT_CONTAINER_NAME), Registry.class, this.registry)
        ;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public void start(StartContext context) {
        Registry<?, ?> registry = this.registry.getValue();
        if (registry.getGroup().getLocalNode().getSocketAddress() != null) {
            this.collector.getValue().add(registry);
        }
    }

    @Override
    public void stop(StopContext context) {
        Registry<?, ?> registry = this.registry.getValue();
        if (registry.getGroup().getLocalNode().getSocketAddress() != null) {
            this.collector.getValue().remove(registry);
        }
    }
}
