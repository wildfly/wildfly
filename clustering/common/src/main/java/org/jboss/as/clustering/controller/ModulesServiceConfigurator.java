/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.wildfly.subsystem.resource.ResourceModelResolver;

/**
 * @author Paul Ferraro
 * @deprecated Use {@link ModuleListAttributeDefinition#resolve(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode)} instead.
 */
@Deprecated(forRemoval = true)
public class ModulesServiceConfigurator extends AbstractModulesServiceConfigurator<List<Module>> {

    private final List<Module> defaultModules;

    public ModulesServiceConfigurator(RuntimeCapability<Void> capability, AttributeDefinition attribute, List<Module> defaultModules) {
        super(capability, new ResourceModelResolver<>() {
            @Override
            public List<String> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                List<ModelNode> values = attribute.resolveModelAttribute(context, model).asListOrEmpty();
                return !values.isEmpty() ? values.stream().map(ModelNode::asString).collect(Collectors.toUnmodifiableList()) : List.of();
            }
        });
        this.defaultModules = defaultModules;
    }

    @Override
    public List<Module> apply(List<Module> modules) {
        return modules.isEmpty() ? this.defaultModules : modules;
    }
}
