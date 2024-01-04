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
public class ThreadPoolServiceNameProvider implements ServiceNameProvider {

    private final ServiceName name;

    public ThreadPoolServiceNameProvider(PathAddress address) {
        this(address.getParent(), address.getLastElement());
    }

    public ThreadPoolServiceNameProvider(PathAddress transportAddress, PathElement path) {
        this.name = new SingletonProtocolServiceNameProvider(transportAddress).getServiceName().append(path.getValue());
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }
}
