/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.service;

import org.jboss.as.clustering.controller.BinaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.BinaryServiceNameFactory;
import org.jboss.as.clustering.controller.DefaultableBinaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.BinaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum SingletonCacheRequirement implements DefaultableBinaryServiceNameFactoryProvider {
    @Deprecated(forRemoval = true) SINGLETON_SERVICE_BUILDER_FACTORY(org.wildfly.clustering.singleton.SingletonCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY, SingletonDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY),
    SINGLETON_SERVICE_CONFIGURATOR_FACTORY(org.wildfly.clustering.singleton.SingletonCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY),
    ;
    private final BinaryServiceNameFactory factory;
    private final SingletonDefaultCacheRequirement defaultRequirement;

    SingletonCacheRequirement(BinaryRequirement requirement, SingletonDefaultCacheRequirement defaultRequirement) {
        this.factory = new BinaryRequirementServiceNameFactory(requirement);
        this.defaultRequirement = defaultRequirement;
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
