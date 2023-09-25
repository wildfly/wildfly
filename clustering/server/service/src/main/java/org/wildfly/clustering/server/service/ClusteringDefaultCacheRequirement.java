/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.service;

import java.util.Map;

import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactoryProvider;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum ClusteringDefaultCacheRequirement implements UnaryRequirement, UnaryServiceNameFactoryProvider {
    GROUP("org.wildfly.clustering.cache.default-group", Group.class),
    REGISTRY("org.wildfly.clustering.cache.default-registry", Registry.class),
    REGISTRY_ENTRY("org.wildfly.clustering.cache.default-registry-entry", Map.Entry.class),
    REGISTRY_FACTORY("org.wildfly.clustering.cache.default-registry-factory", RegistryFactory.class),
    SERVICE_PROVIDER_REGISTRY("org.wildfly.clustering.cache.default-service-provider-registry", ServiceProviderRegistry.class),
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
