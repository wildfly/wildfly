/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import org.wildfly.clustering.service.DefaultableBinaryRequirement;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
@Deprecated(forRemoval = true)
public enum SingletonCacheRequirement implements DefaultableBinaryRequirement {

    /**
     * @deprecated Use {@link SingletonServiceBuilderFactory#SERVICE_DESCRIPTOR} instead.
     */
    @Deprecated(forRemoval = true) SINGLETON_SERVICE_BUILDER_FACTORY(SingletonServiceBuilderFactory.SERVICE_DESCRIPTOR, SingletonDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY),
    /**
     * @deprecated Use {@link SingletonServiceConfiguratorFactory#SERVICE_DESCRIPTOR} instead.
     */
    @Deprecated SINGLETON_SERVICE_CONFIGURATOR_FACTORY(SingletonServiceConfiguratorFactory.SERVICE_DESCRIPTOR, SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY),
    ;
    private final BinaryServiceDescriptor<?> descriptor;
    private final UnaryRequirement defaultRequirement;

    SingletonCacheRequirement(BinaryServiceDescriptor<?> descriptor, UnaryRequirement defaultRequirement) {
        this.descriptor = descriptor;
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.descriptor.getName();
    }

    @Override
    public UnaryRequirement getDefaultRequirement() {
        return this.defaultRequirement;
    }
}
