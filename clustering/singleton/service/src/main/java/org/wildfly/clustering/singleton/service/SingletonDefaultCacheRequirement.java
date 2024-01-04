/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.service;

import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactoryProvider;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum SingletonDefaultCacheRequirement implements UnaryServiceNameFactoryProvider {
    @Deprecated(forRemoval = true) SINGLETON_SERVICE_BUILDER_FACTORY(org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY),
    SINGLETON_SERVICE_CONFIGURATOR_FACTORY(org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY),
    ;
    private final UnaryServiceNameFactory factory;

    SingletonDefaultCacheRequirement(UnaryRequirement requirement) {
        this.factory = new UnaryRequirementServiceNameFactory(requirement);
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
