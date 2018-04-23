/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jgroups.protocols.TP;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;

/**
 * Registers transport definitions, including any definition overrides.
 * @author Paul Ferraro
 */
public class TransportRegistration implements Registration<ManagementResourceRegistration> {

    enum MulticastTransport {
        UDP;

        static boolean contains(String name) {
            for (MulticastTransport protocol : EnumSet.allOf(MulticastTransport.class)) {
                if (name.equals(protocol.name())) {
                    return true;
                }
            }
            return false;
        }
    }

    static class TransportResourceServiceBuilderFactory<T extends TP> implements ResourceServiceBuilderFactory<TransportConfiguration<T>> {
        @SuppressWarnings("unchecked")
        @Override
        public ResourceServiceBuilder<TransportConfiguration<T>> createBuilder(PathAddress address) {
            return MulticastTransport.contains(address.getLastElement().getValue()) ? (TransportConfigurationBuilder<T>) new MulticastTransportConfigurationBuilder(address) : new TransportConfigurationBuilder<>(address);
        }
    }

    private final ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory;

    public TransportRegistration(ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        this.parentBuilderFactory = parentBuilderFactory;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        new TransportResourceDefinition<>(new TransportResourceServiceBuilderFactory<>(), this.parentBuilderFactory).register(registration);
    }
}
