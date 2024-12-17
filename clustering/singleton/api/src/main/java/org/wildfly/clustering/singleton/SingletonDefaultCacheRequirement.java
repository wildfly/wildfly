/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
@Deprecated(forRemoval = true)
public enum SingletonDefaultCacheRequirement implements UnaryRequirement {

    /**
     * @deprecated Use {@link SingletonServiceBuilderFactory#DEFAULT_SERVICE_DESCRIPTOR} instead.
     */
    @Deprecated(forRemoval = true) SINGLETON_SERVICE_BUILDER_FACTORY(SingletonServiceBuilderFactory.DEFAULT_SERVICE_DESCRIPTOR),
    /**
     * @deprecated Use {@link SingletonServiceConfiguratorFactory#DEFAULT_SERVICE_DESCRIPTOR} instead.
     */
    @Deprecated SINGLETON_SERVICE_CONFIGURATOR_FACTORY(SingletonServiceConfiguratorFactory.DEFAULT_SERVICE_DESCRIPTOR),
    ;
    private final UnaryServiceDescriptor<?> descriptor;

    SingletonDefaultCacheRequirement(UnaryServiceDescriptor<?> descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public String getName() {
        return this.descriptor.getName();
    }

    @Override
    public Class<?> getType() {
        return this.descriptor.getType();
    }
}
