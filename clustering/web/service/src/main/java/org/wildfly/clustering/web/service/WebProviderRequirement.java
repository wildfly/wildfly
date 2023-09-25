/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service;

import org.jboss.as.clustering.controller.DefaultableUnaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.DefaultableUnaryRequirement;
import org.wildfly.clustering.service.Requirement;

/**
 * @author Paul Ferraro
 */
public enum WebProviderRequirement implements DefaultableUnaryRequirement, DefaultableUnaryServiceNameFactoryProvider {
    SESSION_MANAGEMENT_PROVIDER("org.wildfly.clustering.web.session-management-provider", WebDefaultProviderRequirement.SESSION_MANAGEMENT_PROVIDER),
    SSO_MANAGEMENT_PROVIDER("org.wildfly.clustering.web.single-sign-on-management-provider", WebDefaultProviderRequirement.SSO_MANAGEMENT_PROVIDER),
    AFFINITY("org.wildfly.clustering.web.session-affinity", WebDefaultProviderRequirement.AFFINITY),
    ;
    private final String name;
    private final UnaryServiceNameFactory factory;
    private final WebDefaultProviderRequirement defaultRequirement;

    WebProviderRequirement(String name, WebDefaultProviderRequirement defaultRequirement) {
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
