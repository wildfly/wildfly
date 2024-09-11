/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
@Deprecated(forRemoval = true)
public abstract class AbstractModulesServiceConfigurator<T> implements ResourceServiceConfigurator, Function<List<Module>, T> {

    private final RuntimeCapability<Void> capability;
    private final ResourceModelResolver<List<String>> resolver;

    AbstractModulesServiceConfigurator(RuntimeCapability<Void> capability, ResourceModelResolver<List<String>> resolver) {
        this.capability = capability;
        this.resolver = resolver;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        List<String> moduleIdentifiers = this.resolver.resolve(context, model);
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        Supplier<List<Module>> modules = new Supplier<>() {
            @Override
            public List<Module> get() {
                return moduleIdentifiers.stream().map(this::load).collect(Collectors.toUnmodifiableList());
            }

            private Module load(String identifier) {
                try {
                    return loader.get().loadModule(identifier);
                } catch (ModuleLoadException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        return CapabilityServiceInstaller.builder(this.capability, this, modules)
                .requires(loader)
                .asPassive()
                .build();
    }
}
