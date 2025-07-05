/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.routing;

/**
 * Creates a legacy routing provider.
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyRoutingProviderFactory {
    /**
     * Fabricates a routing provider in the absence of proper routing configuration.
     * @return a routing provider
     */
    RoutingProvider createRoutingProvider();
}
