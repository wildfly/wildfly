/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.Requirement;

/**
 * Interface to be implemented by capability enumerations.
 * @author Paul Ferraro
 */
public interface Capability extends Definable<RuntimeCapability<?>>, Requirement, ResourceServiceNameFactory {

    @Override
    default String getName() {
        return this.getDefinition().getName();
    }

    @Override
    default Class<?> getType() {
        return this.getDefinition().getCapabilityServiceValueType();
    }

    /**
     * Resolves this capability against the specified path address
     * @param address a path address
     * @return a resolved runtime capability
     */
    default RuntimeCapability<?> resolve(PathAddress address) {
        RuntimeCapability<?> definition = this.getDefinition();
        return definition.isDynamicallyNamed() ? definition.fromBaseCapability(address) : definition;
    }

    @Override
    default ServiceName getServiceName(PathAddress address) {
        return this.resolve(address).getCapabilityServiceName();
    }
}
