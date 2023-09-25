/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.ServiceNameProvider;

/**
 * @author Paul Ferraro
 */
public class RemoteSiteServiceNameProvider implements ServiceNameProvider {

    private final ServiceName name;

    public RemoteSiteServiceNameProvider(PathAddress address) {
        this(address.getParent(), address.getLastElement());
    }

    public RemoteSiteServiceNameProvider(PathAddress relayAddress, PathElement path) {
        this.name = new SingletonProtocolServiceNameProvider(relayAddress).getServiceName().append(path.getValue());
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }
}
