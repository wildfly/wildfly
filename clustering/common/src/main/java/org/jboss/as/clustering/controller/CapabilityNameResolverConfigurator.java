/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;

/**
 * Operator that configures a runtime capability with a given capability name resolver.
 * @author Paul Ferraro
 */
public class CapabilityNameResolverConfigurator implements UnaryOperator<RuntimeCapability.Builder<Void>> {

    private final Function<PathAddress, String[]> resolver;

    /**
     * Creates a new capability name resolver configurator
     * @param mapper a capability name resolver
     */
    public CapabilityNameResolverConfigurator(Function<PathAddress, String[]> resolver) {
        this.resolver = resolver;
    }

    @Override
    public RuntimeCapability.Builder<Void> apply(RuntimeCapability.Builder<Void> builder) {
        return builder.setDynamicNameMapper(this.resolver);
    }
}
