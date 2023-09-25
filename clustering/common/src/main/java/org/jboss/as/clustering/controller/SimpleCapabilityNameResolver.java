/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.PathAddress;

/**
 * Dynamic name mapper that uses a static mapping.
 * @author Paul Ferraro
 */
public class SimpleCapabilityNameResolver implements Function<PathAddress, String[]> {
    public static final Function<PathAddress, String[]> EMPTY = new SimpleCapabilityNameResolver();

    private final String[] names;

    public SimpleCapabilityNameResolver(String... names) {
        this.names = names;
    }

    @Override
    public String[] apply(PathAddress address) {
        return this.names;
    }
}
