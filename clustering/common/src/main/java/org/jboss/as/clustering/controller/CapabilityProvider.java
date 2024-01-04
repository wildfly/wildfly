/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.service.ServiceName;

/**
 * Provides a capability instance.
 * Additionally implements {@link Capability}, delegating all methods to the provided capability instance.
 * @author Paul Ferraro
 */
public interface CapabilityProvider extends Capability {

    Capability getCapability();

    @Override
    default RuntimeCapability<?> getDefinition() {
        return this.getCapability().getDefinition();
    }

    @Override
    default RuntimeCapability<?> resolve(PathAddress address) {
        return this.getCapability().resolve(address);
    }

    @Override
    default ServiceName getServiceName(PathAddress address) {
        return this.getCapability().getServiceName(address);
    }
}
