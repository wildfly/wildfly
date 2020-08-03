/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.infinispan;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.network.ClientMapping;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * @author Paul Ferraro
 */
public class ClientMappingsRegistryEntryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<Map.Entry<String, List<ClientMapping>>> {

    private final String containerName;
    private final String cacheName;
    private final SupplierDependency<List<ClientMapping>> clientMappings;
    private volatile SupplierDependency<Group> group;

    public ClientMappingsRegistryEntryServiceConfigurator(String containerName, String cacheName, SupplierDependency<List<ClientMapping>> clientMappings) {
        super(ServiceNameFactory.parseServiceName(ClusteringCacheRequirement.REGISTRY_ENTRY.getName()).append(containerName, cacheName));
        this.containerName = containerName;
        this.cacheName = cacheName;
        this.clientMappings = clientMappings;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.group = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, this.containerName, this.cacheName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<Map.Entry<String, List<ClientMapping>>> entry = new CompositeDependency(this.group, this.clientMappings).register(builder).provides(name);
        Service service = new FunctionalService<>(entry, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Map.Entry<String, List<ClientMapping>> get() {
        return new AbstractMap.SimpleImmutableEntry<>(this.group.get().getLocalMember().getName(), this.clientMappings.get());
    }
}
