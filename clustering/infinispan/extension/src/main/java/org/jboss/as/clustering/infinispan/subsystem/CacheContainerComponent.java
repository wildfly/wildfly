/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.ResourceServiceNameFactory;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 */
public enum CacheContainerComponent implements ResourceServiceNameFactory {

    MODULES("modules"),
    TRANSPORT(JGroupsTransportResourceDefinition.PATH),
    ;
    private final String component;

    CacheContainerComponent(PathElement path) {
        this.component = path.getKey();
    }

    CacheContainerComponent(String component) {
        this.component = component;
    }

    @Override
    public ServiceName getServiceName(PathAddress containerAddress) {
        return CacheContainerResourceDefinition.Capability.CONFIGURATION.getServiceName(containerAddress).append(this.component);
    }
}
