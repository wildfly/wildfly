/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import org.wildfly.clustering.service.DefaultableBinaryRequirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum SingletonCacheRequirement implements DefaultableBinaryRequirement {

    /**
     * @deprecated Use {@link SingletonCacheRequirement#SINGLETON_SERVICE_CONFIGURATOR_FACTORY} instead.
     */
    @Deprecated(forRemoval = true) SINGLETON_SERVICE_BUILDER_FACTORY("org.wildfly.clustering.cache.singleton-service-builder-factory", SingletonDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY),
    SINGLETON_SERVICE_CONFIGURATOR_FACTORY("org.wildfly.clustering.cache.singleton-service-configurator-factory", SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY),
    ;
    private final String name;
    private final UnaryRequirement defaultRequirement;

    SingletonCacheRequirement(String name, UnaryRequirement defaultRequirement) {
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
}
