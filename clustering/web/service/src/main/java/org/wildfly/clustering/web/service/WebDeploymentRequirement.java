/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service;

import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactoryProvider;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.web.routing.RouteLocator;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

/**
 * @author Paul Ferraro
 */
public enum WebDeploymentRequirement implements UnaryRequirement, UnaryServiceNameFactoryProvider {
    LOCAL_ROUTE("org.wildfly.clustering.web.local-route", String.class),
    ROUTE_LOCATOR("org.wildfly.clustering.web.route-locator", RouteLocator.class),
    SESSION_MANAGER_FACTORY("org.wildfly.clustering.web.session-manager-factory", SessionManagerFactory.class),
    SSO_MANAGER_FACTORY("org.wildfly.clustering.web.single-sign-on-manager-factory", SSOManagerFactory.class),
    ;
    private final String name;
    private final Class<?> type;
    private final UnaryServiceNameFactory factory;

    WebDeploymentRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
        this.factory = new UnaryRequirementServiceNameFactory(this);
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
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
