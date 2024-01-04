/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.PathAddress;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.extension.clustering.web.routing.LocalRoutingProvider;

/**
 * Service configurator for the local routing provider.
 * @author Paul Ferraro
 */
public class LocalRoutingProviderServiceConfigurator extends RoutingProviderServiceConfigurator {

    public LocalRoutingProviderServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public RoutingProvider get() {
        return new LocalRoutingProvider();
    }
}
