/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import org.jboss.as.network.SocketBinding;
import org.jgroups.protocols.TP;

/**
 * Defines the configuration of a JGroups transport protocol.
 * @author Paul Ferraro
 */
public interface TransportConfiguration<T extends TP> extends ProtocolConfiguration<T> {

    Topology getTopology();

    SocketBinding getSocketBinding();

    interface Topology {
        String getMachine();
        String getRack();
        String getSite();
    }
}
