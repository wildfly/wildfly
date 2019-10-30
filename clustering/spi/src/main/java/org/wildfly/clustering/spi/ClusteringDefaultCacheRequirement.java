/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.spi;

import java.util.Map;

import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactoryProvider;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement;

/**
 * @author Paul Ferraro
 */
public enum ClusteringDefaultCacheRequirement implements UnaryRequirement, UnaryServiceNameFactoryProvider {
    GROUP("org.wildfly.clustering.cache.default-group", Group.class),
    @Deprecated NODE_FACTORY("org.wildfly.clustering.cache.default-node-factory", org.wildfly.clustering.group.NodeFactory.class),
    REGISTRY("org.wildfly.clustering.cache.default-registry", Registry.class),
    REGISTRY_ENTRY("org.wildfly.clustering.cache.default-registry-entry", Map.Entry.class),
    REGISTRY_FACTORY("org.wildfly.clustering.cache.default-registry-factory", RegistryFactory.class),
    SERVICE_PROVIDER_REGISTRY("org.wildfly.clustering.cache.default-service-provider-registry", ServiceProviderRegistry.class),
    @Deprecated SINGLETON_SERVICE_BUILDER_FACTORY(SingletonDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY),
    SINGLETON_SERVICE_CONFIGURATOR_FACTORY(SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY),
    ;
    private final String name;
    private final Class<?> type;
    private final UnaryServiceNameFactory factory = new UnaryRequirementServiceNameFactory(this);

    ClusteringDefaultCacheRequirement(UnaryRequirement requirement) {
        this(requirement.getName(), requirement.getType());
    }

    ClusteringDefaultCacheRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
