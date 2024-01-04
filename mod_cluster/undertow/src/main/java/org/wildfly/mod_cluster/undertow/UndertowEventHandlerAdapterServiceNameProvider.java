/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition;

/**
 * @author Radoslav Husar
 */
class UndertowEventHandlerAdapterServiceNameProvider implements ServiceNameProvider {

    private final String proxyName;

    UndertowEventHandlerAdapterServiceNameProvider(String proxyName) {
        this.proxyName = proxyName;
    }

    @Override
    public ServiceName getServiceName() {
        return ProxyConfigurationResourceDefinition.Capability.SERVICE.getDefinition().getCapabilityServiceName(proxyName).append("undertow");
    }
}
