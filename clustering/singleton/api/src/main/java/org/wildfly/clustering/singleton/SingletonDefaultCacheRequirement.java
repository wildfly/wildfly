/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;

/**
 * @author Paul Ferraro
 */
public enum SingletonDefaultCacheRequirement implements UnaryRequirement {

    /**
     * @deprecated Use {@link SingletonDefaultCacheRequirement#SINGLETON_SERVICE_CONFIGURATOR_FACTORY} instead.
     */
    @Deprecated(forRemoval = true) SINGLETON_SERVICE_BUILDER_FACTORY("org.wildfly.clustering.cache.default-singleton-service-builder-factory", SingletonServiceBuilderFactory.class),
    SINGLETON_SERVICE_CONFIGURATOR_FACTORY("org.wildfly.clustering.cache.default-singleton-service-configurator-factory", SingletonServiceConfiguratorFactory.class),
    ;
    private final String name;
    private final Class<?> type;

    SingletonDefaultCacheRequirement(String name, Class<?> type) {
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
}
