/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import org.jboss.modules.Module;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Encapsulates the configuration of a JGroups channel.
 * @author Paul Ferraro
 */
public interface ChannelConfiguration {
    NullaryServiceDescriptor<ChannelConfiguration> DEFAULT_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.jgroups.default-channel-configuration", ChannelConfiguration.class);
    UnaryServiceDescriptor<ChannelConfiguration> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.jgroups.channel-configuration", DEFAULT_SERVICE_DESCRIPTOR);

    /**
     * Indicates whether statistics should be enabled.
     * @return true, if statistics will be enabled for this channel, false otherwise.
     */
    boolean isStatisticsEnabled();

    /**
     * The factory for creating this channel.
     * @return a channel factory
     */
    ChannelFactory getChannelFactory();

    /**
     * The module associated with this channel.
     * @return a module
     */
    Module getModule();

    /**
     * The cluster name of this channel.
     * @return a cluster name.
     */
    String getClusterName();
}
