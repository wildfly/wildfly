/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import org.jboss.as.clustering.controller.DefaultableUnaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.DefaultableUnaryRequirement;
import org.wildfly.clustering.service.Requirement;

/**
 * @author Paul Ferraro
 */
public enum ClusteringRequirement implements DefaultableUnaryRequirement, DefaultableUnaryServiceNameFactoryProvider {
    COMMAND_DISPATCHER_FACTORY("org.wildfly.clustering.command-dispatcher-factory", ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY),
    GROUP("org.wildfly.clustering.group", ClusteringDefaultRequirement.GROUP),
    ;
    private final String name;
    private final UnaryServiceNameFactory factory = new UnaryRequirementServiceNameFactory(this);
    private final ClusteringDefaultRequirement defaultRequirement;

    ClusteringRequirement(String name, ClusteringDefaultRequirement defaultRequirement) {
        this.name = name;
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Requirement getDefaultRequirement() {
        return this.defaultRequirement;
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }

    @Override
    public ServiceNameFactory getDefaultServiceNameFactory() {
        return this.defaultRequirement;
    }
}
