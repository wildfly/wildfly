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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * Registers transport definitions, including any definition overrides.
 * @author Paul Ferraro
 */
public class TransportRegistration implements Registration<ManagementResourceRegistration> {

    enum TransportType implements Iterable<String> {
        MULTICAST("UDP"),
        ;
        private Set<String> transports;

        TransportType(String transport) {
            this.transports = Collections.singleton(transport);
        }

        TransportType(String... transports) {
            this.transports = Collections.unmodifiableSet(Stream.of(transports).collect(Collectors.toSet()));
        }

        @Override
        public Iterator<String> iterator() {
            return this.transports.iterator();
        }

        Stream<String> stream() {
            return this.transports.stream();
        }

        boolean contains(String transport) {
            return this.transports.contains(transport);
        }
    }

    private final ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory;

    public TransportRegistration(ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        this.parentBuilderFactory = parentBuilderFactory;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        new TransportResourceDefinition<>(address -> TransportType.MULTICAST.contains(address.getLastElement().getValue()) ? new MulticastTransportConfigurationBuilder<>(address) : new TransportConfigurationBuilder<>(address), this.parentBuilderFactory).register(registration);
    }
}
