/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.wildfly.subsystem.resource.ResourceModelResolver;

/**
 * Configures a service providing a {@link Module}.
 * @author Paul Ferraro
 * @deprecated Use {@link ModuleAttributeDefinition#resolve(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode)} instead.
 */
@Deprecated(forRemoval = true)
public class ModuleServiceConfigurator extends AbstractModulesServiceConfigurator<Module> {

    public ModuleServiceConfigurator(RuntimeCapability<Void> capability, AttributeDefinition attribute) {
        super(capability, new ResourceModelResolver<>() {
            @Override
            public List<String> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                String module = attribute.resolveModelAttribute(context, model).asStringOrNull();
                return (module != null) ? List.of(module) : List.of();
            }
        });
    }

    @Override
    public Module apply(List<Module> modules) {
        return !modules.isEmpty() ? modules.get(0) : null;
    }
}
