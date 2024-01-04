/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
