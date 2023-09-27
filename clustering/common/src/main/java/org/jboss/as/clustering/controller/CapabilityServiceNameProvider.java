/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.ServiceNameProvider;

/**
 * Service name provider for a capability.
 * @author Paul Ferraro
 */
public class CapabilityServiceNameProvider implements ServiceNameProvider {

    private final ServiceName name;

    public CapabilityServiceNameProvider(Capability capability, PathAddress address) {
        this.name = capability.getServiceName(address);
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }
}
