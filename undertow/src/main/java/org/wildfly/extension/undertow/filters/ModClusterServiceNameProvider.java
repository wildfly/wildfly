/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import org.jboss.as.controller.PathAddress;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.extension.undertow.UndertowService;

/**
 * Provides the service name for {@link io.undertow.server.handlers.proxy.mod_cluster.ModCluster}.
 * @author Paul Ferraro
 */
public class ModClusterServiceNameProvider extends SimpleServiceNameProvider {

    public ModClusterServiceNameProvider(PathAddress address) {
        super(UndertowService.FILTER.append(address.getLastElement().getValue(), "service"));
    }
}
