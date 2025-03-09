/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import org.jboss.modules.Module;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 *
 */
public interface ForkChannelConfiguration extends ChannelConfiguration {
    NullaryServiceDescriptor<ForkChannelConfiguration> DEFAULT_SERVICE_DESCRIPTOR = ChannelConfiguration.DEFAULT_SERVICE_DESCRIPTOR.asType(ForkChannelConfiguration.class);
    UnaryServiceDescriptor<ForkChannelConfiguration> SERVICE_DESCRIPTOR = ChannelConfiguration.SERVICE_DESCRIPTOR.asType(ForkChannelConfiguration.class);

    @Override
    ForkChannelFactory getChannelFactory();

    @Override
    default boolean isStatisticsEnabled() {
        return this.getChannelFactory().getConfiguration().getChannelConfiguration().isStatisticsEnabled();
    }

    @Override
    default Module getModule() {
        return this.getChannelFactory().getConfiguration().getChannelConfiguration().getModule();
    }

    @Override
    default String getClusterName() {
        return this.getChannelFactory().getConfiguration().getChannelConfiguration().getClusterName();
    }
}
