/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * Configuration of a channel to a remote site, used by the RELAY2 protocol.
 * @author Paul Ferraro
 */
public interface RemoteSiteConfiguration {
    BinaryServiceDescriptor<RemoteSiteConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.clustering.jgroups.remote-site", RemoteSiteConfiguration.class);

    String getName();

    ChannelFactory getChannelFactory();

    String getClusterName();
}
