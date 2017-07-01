/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import java.util.List;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.network.ClientMapping;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Builds a service providing client mappings for a remote EJB connector.
 * @author Paul Ferraro
 */
public class ClientMappingsRegistryBuilder implements CapabilityServiceBuilder<Registry<String, List<ClientMapping>>> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "remoting", "connector", "client-mappings");

    private final String clientMappingsClusterName;

    @SuppressWarnings("rawtypes")
    private volatile ValueDependency<Registry> registry;

    public ClientMappingsRegistryBuilder(String clientMappingsClusterName) {
        this.clientMappingsClusterName = clientMappingsClusterName;
    }

    @Override
    public ServiceName getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public Builder<Registry<String, List<ClientMapping>>> configure(OperationContext context) {
        this.registry = new InjectedValueDependency<>(ClusteringCacheRequirement.REGISTRY.getServiceName(context, this.clientMappingsClusterName, BeanManagerFactoryBuilderConfiguration.CLIENT_MAPPINGS_CACHE_NAME), Registry.class);
        return this;
    }

    @Override
    public ServiceBuilder<Registry<String, List<ClientMapping>>> build(ServiceTarget target) {
        Value<Registry<String, List<ClientMapping>>> value = () -> this.registry.getValue();
        return this.registry.register(target.addService(this.getServiceName(), new ValueService<>(value)));
    }
}
