/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.jgroups.spi;

import org.jboss.as.clustering.controller.DefaultableUnaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.DefaultableUnaryRequirement;
import org.wildfly.clustering.service.Requirement;

/**
 * @author Paul Ferraro
 */
public enum JGroupsRequirement implements DefaultableUnaryRequirement, DefaultableUnaryServiceNameFactoryProvider {
    CHANNEL("org.wildfly.clustering.jgroups.channel", JGroupsDefaultRequirement.CHANNEL),
    CHANNEL_CLUSTER("org.wildfly.clustering.jgroups.channel-cluster", JGroupsDefaultRequirement.CHANNEL_CLUSTER),
    CHANNEL_FACTORY("org.wildfly.clustering.jgroups.channel-factory", JGroupsDefaultRequirement.CHANNEL_FACTORY),
    CHANNEL_MODULE("org.wildfly.clustering.jgroups.channel-module", JGroupsDefaultRequirement.CHANNEL_MODULE),
    CHANNEL_SOURCE("org.wildfly.clustering.jgroups.channel-source", JGroupsDefaultRequirement.CHANNEL_SOURCE),
    ;
    private final String name;
    private final UnaryServiceNameFactory factory = new UnaryRequirementServiceNameFactory(this);
    private final JGroupsDefaultRequirement defaultRequirement;

    JGroupsRequirement(String name, JGroupsDefaultRequirement defaultRequirement) {
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
