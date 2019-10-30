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

import org.jboss.as.clustering.controller.IdentityCapabilityServiceConfigurator;
import org.jboss.as.network.ClientMapping;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Builds a service providing client mappings for a remote EJB connector.
 * @author Paul Ferraro
 */
public class ClientMappingsRegistryServiceConfigurator extends IdentityCapabilityServiceConfigurator<Registry<String, List<ClientMapping>>> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "remoting", "connector", "client-mappings");

    public ClientMappingsRegistryServiceConfigurator(String clientMappingsClusterName) {
        super(SERVICE_NAME, ClusteringCacheRequirement.REGISTRY, clientMappingsClusterName, BeanManagerFactoryServiceConfiguratorConfiguration.CLIENT_MAPPINGS_CACHE_NAME);
    }
}
