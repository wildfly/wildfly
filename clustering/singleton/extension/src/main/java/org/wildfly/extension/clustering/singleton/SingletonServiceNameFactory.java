/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.DefaultableUnaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.RequirementServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.DefaultableUnaryRequirement;
import org.wildfly.clustering.singleton.SingletonRequirement;

/**
 * @author Paul Ferraro
 */
public enum SingletonServiceNameFactory implements DefaultableUnaryServiceNameFactoryProvider {
    @Deprecated LEGACY_SINGLETON_POLICY(SingletonRequirement.SINGLETON_POLICY),
    SINGLETON_POLICY(SingletonRequirement.POLICY),
    ;
    private final UnaryServiceNameFactory factory;
    private final ServiceNameFactory defaultFactory;

    SingletonServiceNameFactory(DefaultableUnaryRequirement requirement) {
        this.factory = new UnaryRequirementServiceNameFactory(requirement);
        this.defaultFactory = new RequirementServiceNameFactory(requirement.getDefaultRequirement());
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }

    @Override
    public ServiceNameFactory getDefaultServiceNameFactory() {
        return this.defaultFactory;
    }
}
