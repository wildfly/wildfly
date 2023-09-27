/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.service;

import org.jboss.as.clustering.controller.BinaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.BinaryServiceNameFactory;
import org.jboss.as.clustering.controller.DefaultableBinaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.service.DefaultableBinaryRequirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum ClusteringCacheRequirement implements DefaultableBinaryRequirement, DefaultableBinaryServiceNameFactoryProvider {
    GROUP("org.wildfly.clustering.cache.group", ClusteringDefaultCacheRequirement.GROUP),
    REGISTRY("org.wildfly.clustering.cache.registry", ClusteringDefaultCacheRequirement.REGISTRY),
    REGISTRY_ENTRY("org.wildfly.clustering.cache.registry-entry", ClusteringDefaultCacheRequirement.REGISTRY_ENTRY),
    REGISTRY_FACTORY("org.wildfly.clustering.cache.registry-factory", ClusteringDefaultCacheRequirement.REGISTRY_FACTORY),
    SERVICE_PROVIDER_REGISTRY("org.wildfly.clustering.cache.service-provider-registry", ClusteringDefaultCacheRequirement.SERVICE_PROVIDER_REGISTRY),
    ;
    private final String name;
    private final BinaryServiceNameFactory factory = new BinaryRequirementServiceNameFactory(this);
    private final ClusteringDefaultCacheRequirement defaultRequirement;

    ClusteringCacheRequirement(BinaryRequirement requirement, ClusteringDefaultCacheRequirement defaultRequirement) {
        this(requirement.getName(), defaultRequirement);
    }

    ClusteringCacheRequirement(String name, ClusteringDefaultCacheRequirement defaultRequirement) {
        this.name = name;
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public UnaryRequirement getDefaultRequirement() {
        return this.defaultRequirement;
    }

    @Override
    public BinaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }

    @Override
    public UnaryServiceNameFactory getDefaultServiceNameFactory() {
        return this.defaultRequirement;
    }
}
