/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import java.util.List;

import org.jgroups.protocols.relay.RELAY2;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Configuration of the RELAY2 protocol.
 * @author Paul Ferraro
 */
public interface RelayConfiguration extends ProtocolConfiguration<RELAY2> {
    UnaryServiceDescriptor<RelayConfiguration> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.jgroups.relay", RelayConfiguration.class);

    String PROTOCOL_NAME = "relay.RELAY2";

    String getSiteName();
    List<RemoteSiteConfiguration> getRemoteSites();
}
