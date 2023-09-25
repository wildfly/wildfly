/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service;

import org.jboss.as.clustering.controller.RequirementServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactoryProvider;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.web.service.routing.RouteLocatorServiceConfiguratorFactory;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.service.sso.DistributableSSOManagementProvider;

/**
 * @author Paul Ferraro
 */
public enum WebDefaultProviderRequirement implements Requirement, ServiceNameFactoryProvider {
    SESSION_MANAGEMENT_PROVIDER("org.wildfly.clustering.web.default-session-management-provider", DistributableSessionManagementProvider.class),
    SSO_MANAGEMENT_PROVIDER("org.wildfly.clustering.web.default-single-sign-on-management-provider", DistributableSSOManagementProvider.class),
    AFFINITY("org.wildfly.clustering.web.default-session-affinity", RouteLocatorServiceConfiguratorFactory.class),
    ;
    private final String name;
    private final Class<?> type;
    private final ServiceNameFactory factory;

    WebDefaultProviderRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
        this.factory = new RequirementServiceNameFactory(this);
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
    public ServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
