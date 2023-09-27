/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.routing;

import java.util.AbstractMap.SimpleImmutableEntry;

/**
 * Registry entry for the Infinispan routing provider.
 * @author Paul Ferraro
 */
public class RouteRegistryEntry extends SimpleImmutableEntry<String, Void> {
    private static final long serialVersionUID = 6829830614436225931L;

    public RouteRegistryEntry(String route) {
        super(route, null);
    }
}
