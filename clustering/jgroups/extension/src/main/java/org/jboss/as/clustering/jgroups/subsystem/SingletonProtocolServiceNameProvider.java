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
public class SingletonProtocolServiceNameProvider implements ServiceNameProvider {

    private final ServiceName name;

    public SingletonProtocolServiceNameProvider(PathAddress address) {
        this(address.getParent(), address.getLastElement());
    }

    public SingletonProtocolServiceNameProvider(PathAddress stackAddress, PathElement path) {
        this.name = StackResourceDefinition.Capability.JCHANNEL_FACTORY.getServiceName(stackAddress).append(path.getKey());
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }
}
