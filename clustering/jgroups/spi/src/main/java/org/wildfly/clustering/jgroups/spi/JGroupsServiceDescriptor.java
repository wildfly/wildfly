/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import org.jgroups.JChannel;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
public interface JGroupsServiceDescriptor {
    NullaryServiceDescriptor<JChannel> DEFAULT_CHANNEL = NullaryServiceDescriptor.of("org.wildfly.clustering.jgroups.default-channel", JChannel.class);
    UnaryServiceDescriptor<JChannel> CHANNEL = UnaryServiceDescriptor.of("org.wildfly.clustering.jgroups.channel", DEFAULT_CHANNEL);
}
