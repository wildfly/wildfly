/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

import org.jboss.as.clustering.controller.DefaultableUnaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.DefaultableUnaryRequirement;
import org.wildfly.clustering.service.Requirement;

/**
 * Requirement definition for EJB abstractions.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum BeanProviderRequirement implements DefaultableUnaryRequirement, DefaultableUnaryServiceNameFactoryProvider {
    BEAN_MANAGEMENT_PROVIDER("org.wildfly.clustering.ejb.bean-management-provider", DefaultBeanProviderRequirement.BEAN_MANAGEMENT_PROVIDER),
    ;
    private final String name;
    private final UnaryServiceNameFactory factory;
    private final DefaultBeanProviderRequirement defaultRequirement;

    BeanProviderRequirement(String name, DefaultBeanProviderRequirement defaultRequirement) {
        this.name = name;
        this.factory = new UnaryRequirementServiceNameFactory(this);
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }

    @Override
    public ServiceNameFactory getDefaultServiceNameFactory() {
        return this.defaultRequirement;
    }

    @Override
    public Requirement getDefaultRequirement() {
        return this.defaultRequirement;
    }
}
