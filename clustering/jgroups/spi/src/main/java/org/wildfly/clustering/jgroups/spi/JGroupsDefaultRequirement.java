/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import org.jboss.as.clustering.controller.RequirementServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactoryProvider;
import org.jboss.modules.Module;
import org.jgroups.JChannel;
import org.wildfly.clustering.service.Requirement;

/**
 * @author Paul Ferraro
 */
public enum JGroupsDefaultRequirement implements Requirement, ServiceNameFactoryProvider {
    CHANNEL("org.wildfly.clustering.jgroups.default-channel", JChannel.class),
    CHANNEL_CLUSTER("org.wildfly.clustering.jgroups.default-channel-cluster", String.class),
    CHANNEL_FACTORY("org.wildfly.clustering.jgroups.default-channel-factory", ChannelFactory.class),
    CHANNEL_MODULE("org.wildfly.clustering.jgroups.default-channel-module", Module.class),
    CHANNEL_SOURCE("org.wildfly.clustering.jgroups.default-channel-source", ChannelFactory.class),
    ;
    private final String name;
    private final Class<?> type;
    private final ServiceNameFactory factory = new RequirementServiceNameFactory(this);

    JGroupsDefaultRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
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
