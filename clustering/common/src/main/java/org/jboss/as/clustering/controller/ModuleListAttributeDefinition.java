/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.ModuleNameValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Attribute definition that resolves to a {@link ServiceDependency} on a list of modules.
 * @author Paul Ferraro
 */
public class ModuleListAttributeDefinition extends StringListAttributeDefinition implements ResourceModelResolver<ServiceDependency<List<Module>>> {

    ModuleListAttributeDefinition(Builder builder) {
        super(builder);
    }

    @Override
    public ServiceDependency<List<Module>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        List<ModelNode> values = this.resolveModelAttribute(context, model).asListOrEmpty();
        if (values.isEmpty()) return ServiceDependency.of(List.<Module>of());
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        return loader.map(new Function<>() {
            @Override
            public List<Module> apply(ModuleLoader loader) {
                List<Module> modules = new ArrayList<>(values.size());
                for (ModelNode value : values) {
                    try {
                        modules.add(loader.loadModule(value.asString()));
                    } catch (ModuleLoadException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
                return modules;
            }
        });
    }

    public static class Builder extends ListAttributeDefinition.Builder<Builder, ModuleListAttributeDefinition> {

        public Builder() {
            this("modules");
        }

        public Builder(String attributeName) {
            super(attributeName);
            // Capability references never allow expressions
            this.setAllowExpression(true);
            this.setAttributeMarshaller(AttributeMarshaller.STRING_LIST);
            this.setAttributeParser(AttributeParser.STRING_LIST);
            this.setElementValidator(ModuleNameValidator.INSTANCE);
            this.setFlags(Flag.RESTART_RESOURCE_SERVICES);
        }

        public Builder(String attributeName, ListAttributeDefinition basis) {
            super(attributeName, basis);
        }

        public Builder setDefaultValue(Module defaultModule) {
            if (defaultModule != null) {
                this.setDefaultValue(List.of(defaultModule));
            }
            return this;
        }

        public Builder setDefaultValue(List<Module> defaultModules) {
            ModelNode list = new ModelNode().setEmptyList();
            for (Module module : defaultModules) {
                list.add(module.getName());
            }
            this.setRequired(false);
            this.setDefaultValue(list);
            return this;
        }

        @Override
        public ModuleListAttributeDefinition build() {
            return new ModuleListAttributeDefinition(this);
        }
    }
}
