/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.jboss.as.clustering.controller.ResourceServiceNameFactory;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.msc.service.ServiceName;

/**
 * Enumerates components of the remote cache container.
 *
 * @author Radoslav Husar
 */
public enum RemoteCacheContainerComponent implements ResourceServiceNameFactory {

    CONNECTION_POOL(ConnectionPoolResourceDefinition.PATH),
    MODULES("modules"),
    SECURITY(SecurityResourceDefinition.PATH),
    ;

    private final String[] components;

    RemoteCacheContainerComponent(PathElement path) {
        this(path.isWildcard() ? path.getKey() : path.getValue());
    }

    RemoteCacheContainerComponent(String... components) {
        this.components = components;
    }

    @Override
    public ServiceName getServiceName(PathAddress remoteContainerAddress) {
        return RemoteCacheContainerResourceDefinition.Capability.CONFIGURATION.getServiceName(remoteContainerAddress).append(this.components);
    }
}
