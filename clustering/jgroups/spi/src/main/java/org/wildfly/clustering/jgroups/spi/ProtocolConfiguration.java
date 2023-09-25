/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import java.util.Map;

import org.jboss.as.network.SocketBinding;
import org.jgroups.stack.Protocol;

/**
 * Defines the configuration of a JGroups protocol.
 * @author Paul Ferraro
 */
public interface ProtocolConfiguration<P extends Protocol> {

    String getName();

    P createProtocol(ProtocolStackConfiguration stackConfiguration);

    default Map<String, SocketBinding> getSocketBindings() {
        return Map.of();
    }
}
