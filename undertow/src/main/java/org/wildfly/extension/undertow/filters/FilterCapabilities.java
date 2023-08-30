/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.filters;

import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_FILTER;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_HOST_FILTER_REF;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_LOCATION_FILTER_REF;

import java.util.function.Function;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.extension.undertow.UndertowFilter;

import io.undertow.server.HandlerWrapper;

/**
 * @author Paul Ferraro
 * @author baranowb
 */
enum FilterCapabilities implements Capability {

    FILTER_CAPABILITY(CAPABILITY_FILTER, HandlerWrapper.class),
    //TODO: switch TernaryCapabilityNameResolver
    FILTER_HOST_REF_CAPABILITY(CAPABILITY_HOST_FILTER_REF, UndertowFilter.class,new Function<PathAddress, String[]>() {

        @Override
        public String[] apply(PathAddress address) {
            // /subsystem=undertow/server=default-server/host=fridge-server/filter-ref=XXX
            // org.wildfly.extension.undertow.filter-ref.default-server.fridge-server.XXXX
            PathAddress parent = address.getParent();
            return new String[] { parent.getParent().getLastElement().getValue(), parent.getLastElement().getValue(), address.getLastElement().getValue() };
        }
    }),
    //TODO: switch QuaternaryCapabilityNameResolver
    FILTER_LOCATION_REF_CAPABILITY(CAPABILITY_LOCATION_FILTER_REF, UndertowFilter.class,new Function<PathAddress, String[]>() {

        @Override
        public String[] apply(PathAddress address) {
            // /subsystem=undertow/server=default-server/host=fridge-server/location=test/filter-ref=XXX
            // org.wildfly.extension.undertow.filter-ref.default-server.fridge-server.test.XXXX
            PathAddress parent = address.getParent();
            PathAddress grandparent = address.getParent().getParent();
            return new String[] { grandparent.getParent().getLastElement().getValue(), grandparent.getLastElement().getValue(), parent.getLastElement().getValue(), address.getLastElement().getValue() };
        }
    });

    private final RuntimeCapability<Void> definition;

    FilterCapabilities(String name, Class<?> serviceValueType) {
        this.definition = RuntimeCapability.Builder.of(name, true, serviceValueType)
                .setDynamicNameMapper(UnaryCapabilityNameResolver.DEFAULT).build();
    }

    FilterCapabilities(String name, Class<?> serviceValueType, Function<PathAddress, String[]> nameResolver) {
        this.definition = RuntimeCapability.Builder.of(name, true, serviceValueType)
                .setDynamicNameMapper(nameResolver).build();
    }

    @Override
    public RuntimeCapability<Void> getDefinition() {
        return this.definition;
    }
}
