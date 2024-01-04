/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import java.util.List;

import org.jgroups.protocols.relay.RELAY2;

/**
 * Configuration of the RELAY2 protocol.
 * @author Paul Ferraro
 */
public interface RelayConfiguration extends ProtocolConfiguration<RELAY2> {

    String PROTOCOL_NAME = "relay.RELAY2";

    String getSiteName();
    List<RemoteSiteConfiguration> getRemoteSites();
}
