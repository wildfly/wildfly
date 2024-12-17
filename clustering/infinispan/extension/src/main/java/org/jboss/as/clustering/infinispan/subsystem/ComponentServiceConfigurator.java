/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.commons.configuration.Builder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Configures a service providing a component.
 * @author Paul Ferraro
 */
public class ComponentServiceConfigurator<C, B extends Builder<C>> implements ResourceServiceConfigurator {

    private final ComponentResourceDescription<C, B> description;

    public ComponentServiceConfigurator(ComponentResourceDescription<C, B> description) {
        this.description = description;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(this.description.getCapability(), this.description.resolve(context, model).map(Builder::create)).build();
    }
}
