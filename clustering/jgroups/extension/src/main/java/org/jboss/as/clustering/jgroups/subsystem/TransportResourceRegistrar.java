/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.ManagementRegistrar;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Registers transport definitions, including any definition overrides.
 * @author Paul Ferraro
 */
public enum TransportResourceRegistrar implements ManagementRegistrar<ManagementResourceRegistration> {
    INSTANCE;

    enum MulticastTransport {
        UDP;
    }

    enum SocketTransport {
        TCP, TCP_NIO2;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        new TransportResourceDefinition<>().register(registration);

        for (MulticastTransport transport : EnumSet.allOf(MulticastTransport.class)) {
            new MulticastSocketTransportResourceDefinition(transport.name()).register(registration);
        }

        for (SocketTransport transport : EnumSet.allOf(SocketTransport.class)) {
            new SocketTransportResourceDefinition<>(transport.name()).register(registration);
        }
    }
}
