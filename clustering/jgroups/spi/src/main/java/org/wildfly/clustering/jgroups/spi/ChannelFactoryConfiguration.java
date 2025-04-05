/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import java.util.List;
import java.util.Optional;

import org.jboss.as.network.SocketBindingManager;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;

/**
 * Defines the configuration of a JGroups protocol stack.
 * @author Paul Ferraro
 */
public interface ChannelFactoryConfiguration {

    String getMemberName();

    boolean isStatisticsEnabled();

    TransportConfiguration<? extends TP> getTransport();

    List<ProtocolConfiguration<? extends Protocol>> getProtocols();

    Optional<RelayConfiguration> getRelay();

    SocketBindingManager getSocketBindingManager();
}
