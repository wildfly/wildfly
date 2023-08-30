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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.QuaternaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.TernaryCapabilityNameResolver;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.wildfly.extension.undertow.UndertowFilter;

/**
 * @author Paul Ferraro
 * @author baranowb
 */
enum FilterCapabilities implements Capability {

    FILTER_CAPABILITY(CAPABILITY_FILTER, PredicateHandlerWrapper.class),
    FILTER_HOST_REF_CAPABILITY(CAPABILITY_HOST_FILTER_REF, UndertowFilter.class, TernaryCapabilityNameResolver.GRANDPARENT_PARENT_CHILD),
    FILTER_LOCATION_REF_CAPABILITY(CAPABILITY_LOCATION_FILTER_REF, UndertowFilter.class, QuaternaryCapabilityNameResolver.GREATGRANDPARENT_GRANDPARENT_PARENT_CHILD);

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
