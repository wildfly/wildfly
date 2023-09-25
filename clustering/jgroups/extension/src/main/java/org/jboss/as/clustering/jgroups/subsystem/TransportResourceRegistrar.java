/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.ManagementRegistrar;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Registers transport definitions, including any definition overrides.
 * @author Paul Ferraro
 */
public class TransportResourceRegistrar implements ManagementRegistrar<ManagementResourceRegistration> {

    enum MulticastTransport {
        UDP;
    }

    enum SocketTransport {
        TCP, TCP_NIO2;
    }

    private final ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory;

    public TransportResourceRegistrar(ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        this.parentServiceConfiguratorFactory = parentServiceConfiguratorFactory;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        new TransportResourceDefinition(this.parentServiceConfiguratorFactory).register(registration);

        for (MulticastTransport transport : EnumSet.allOf(MulticastTransport.class)) {
            new TransportResourceDefinition(transport.name(), MulticastTransportConfigurationServiceConfigurator::new, this.parentServiceConfiguratorFactory).register(registration);
        }

        for (SocketTransport transport : EnumSet.allOf(SocketTransport.class)) {
            new SocketTransportResourceDefinition(transport.name(), SocketTransportConfigurationServiceConfigurator::new, this.parentServiceConfiguratorFactory).register(registration);
        }
    }
}
