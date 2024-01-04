/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service;

import org.jboss.as.clustering.controller.RequirementServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactoryProvider;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.web.service.routing.RoutingProvider;

/**
 * Defines capability names common to a web container.
 * @author Paul Ferraro
 */
public enum WebRequirement implements Requirement, ServiceNameFactoryProvider {
    ROUTING_PROVIDER("org.wildfly.clustering.web.routing-provider", RoutingProvider.class),
    INFINISPAN_ROUTING_PROVIDER("org.wildfly.clustering.web.infinispan-routing-provider", RoutingProvider.class),
    ;
    private final String name;
    private final Class<?> type;
    private final ServiceNameFactory factory;

    WebRequirement(String name, Class<?> type) {
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
