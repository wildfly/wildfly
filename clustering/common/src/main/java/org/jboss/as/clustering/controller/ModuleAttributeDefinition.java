/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ModuleNameValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Attribute definition that resolves to a {@link ServiceDependency} on a module.
 * @author Paul Ferraro
 */
public class ModuleAttributeDefinition extends SimpleAttributeDefinition implements ResourceModelResolver<ServiceDependency<Module>> {

    ModuleAttributeDefinition(Builder builder) {
        super(builder);
    }

    @Override
    public ServiceDependency<Module> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String moduleId = this.resolveModelAttribute(context, model).asStringOrNull();
        if (moduleId == null) return ServiceDependency.of(null);
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        return loader.map(new Function<>() {
            @Override
            public Module apply(ModuleLoader loader) {
                try {
                    return loader.loadModule(moduleId);
                } catch (ModuleLoadException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, ModuleAttributeDefinition> {

        public Builder() {
            this(ModelDescriptionConstants.MODULE);
        }

        public Builder(String attributeName) {
            super(attributeName, ModelType.STRING);
            this.setAllowExpression(true);
            this.setAttributeParser(AttributeParser.SIMPLE);
            this.setFlags(Flag.RESTART_RESOURCE_SERVICES);
        }

        public Builder(String attributeName, ModuleAttributeDefinition basis) {
            super(attributeName, basis);
        }

        public Builder setDefaultValue(Module defaultModule) {
            this.setRequired(false);
            return this.setDefaultValue((defaultModule != null) ? new ModelNode(defaultModule.getName()) : null);
        }

        @Override
        public ModuleAttributeDefinition build() {
            this.setValidator(ModuleNameValidator.INSTANCE);
            return new ModuleAttributeDefinition(this);
        }
    }
}
