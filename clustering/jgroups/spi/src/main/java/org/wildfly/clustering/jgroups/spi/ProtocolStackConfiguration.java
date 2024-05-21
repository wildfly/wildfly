/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import java.util.Optional;

import org.jboss.as.network.SocketBindingManager;
import org.jgroups.protocols.TP;

/**
 * Defines the configuration of a JGroups protocol stack.
 * @author Paul Ferraro
 */
public interface ProtocolStackConfiguration extends StackConfiguration {

    String getMemberName();

    boolean isStatisticsEnabled();

    TransportConfiguration<? extends TP> getTransport();

    Optional<RelayConfiguration> getRelay();

    SocketBindingManager getSocketBindingManager();
}
