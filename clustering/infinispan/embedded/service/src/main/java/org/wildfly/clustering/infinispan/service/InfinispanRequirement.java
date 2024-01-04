/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.service;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactoryProvider;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum InfinispanRequirement implements UnaryRequirement, UnaryServiceNameFactoryProvider {

    CONTAINER("org.wildfly.clustering.infinispan.cache-container", EmbeddedCacheManager.class),
    CONFIGURATION("org.wildfly.clustering.infinispan.cache-container-configuration", GlobalConfiguration.class),
    KEY_AFFINITY_FACTORY("org.wildfly.clustering.infinispan.key-affinity-factory", KeyAffinityServiceFactory.class),
    ;
    private final String name;
    private final Class<?> type;
    private final UnaryServiceNameFactory factory = new UnaryRequirementServiceNameFactory(this);

    InfinispanRequirement(String name, Class<?> type) {
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
