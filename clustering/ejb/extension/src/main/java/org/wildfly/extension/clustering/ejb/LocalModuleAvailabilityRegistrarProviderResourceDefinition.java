/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

import java.util.function.UnaryOperator;

/**
 * Definition of the /subsystem=distributable-ejb/module-availability-registrar=local resource.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class LocalModuleAvailabilityRegistrarProviderResourceDefinition extends ModuleAvailabilityRegistrarProviderResourceDefinition {

    static final PathElement PATH = pathElement("local");

    LocalModuleAvailabilityRegistrarProviderResourceDefinition() {
        super(PATH, UnaryOperator.identity());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(CAPABILITY, LocalModuleAvailabilityRegistrarProvider.INSTANCE).build();
    }
}
