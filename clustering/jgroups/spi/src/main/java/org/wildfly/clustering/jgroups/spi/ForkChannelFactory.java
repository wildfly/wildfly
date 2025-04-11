/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import org.jgroups.Message;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 *
 */
public interface ForkChannelFactory extends ChannelFactory {
    NullaryServiceDescriptor<ForkChannelFactory> DEFAULT_SERVICE_DESCRIPTOR = ChannelFactory.DEFAULT_SERVICE_DESCRIPTOR.asType(ForkChannelFactory.class);
    UnaryServiceDescriptor<ForkChannelFactory> SERVICE_DESCRIPTOR = ChannelFactory.SERVICE_DESCRIPTOR.asType(ForkChannelFactory.class);

    @Override
    ForkChannelFactoryConfiguration getConfiguration();

    @Override
    default boolean isUnknownForkResponse(Message response) {
        return this.getConfiguration().getChannelConfiguration().getChannelFactory().isUnknownForkResponse(response);
    }
}
