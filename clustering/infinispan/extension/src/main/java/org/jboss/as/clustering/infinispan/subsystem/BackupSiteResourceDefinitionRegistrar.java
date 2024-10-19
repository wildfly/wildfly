/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.BackupConfigurationBuilder;
import org.jboss.as.clustering.infinispan.subsystem.BackupSiteResourceDescription.DeprecatedAttribute;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.executor.RuntimeOperationStepHandler;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Definition of a backup site resource.
 *
 * @author Paul Ferraro
 */
public class BackupSiteResourceDefinitionRegistrar extends ComponentServiceConfigurator<BackupConfiguration, BackupConfigurationBuilder> implements ChildResourceDefinitionRegistrar {

    private final ResourceOperationRuntimeHandler parentRuntimeHandler;
    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    BackupSiteResourceDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(BackupSiteResourceDescription.INSTANCE);
        this.parentRuntimeHandler = parentRuntimeHandler;
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(BackupSiteResourceDescription.INSTANCE.getPathElement());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(resolver)
                .addAttributes(BackupSiteResourceDescription.INSTANCE.getAttributes().filter(Predicate.not(DeprecatedAttribute.ENABLED.get()::equals)).collect(Collectors.toUnmodifiableList()))
                .provideModelOnlyAttributes(EnumSet.allOf(BackupSiteResourceDescription.DeprecatedAttribute.class))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.combine(ResourceOperationRuntimeHandler.configureService(this), ResourceOperationRuntimeHandler.restartParent(this.parentRuntimeHandler)))
                .build();

        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(BackupSiteResourceDescription.INSTANCE, resolver).build());

        ManagementResourceRegistrar.of(descriptor).register(registration);

        if (context.isRuntimeOnlyRegistrationValid()) {
            new RuntimeOperationStepHandler<>(new BackupSiteOperationExecutor(this.executors), BackupSiteOperation.class).register(registration);
        }

        return registration;
    }
}
