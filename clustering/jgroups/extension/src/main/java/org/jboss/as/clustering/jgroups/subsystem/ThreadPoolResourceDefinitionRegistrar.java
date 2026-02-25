/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.DurationAttributeDefinition;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for a JGroups thread pool.
 * @author Radoslav Husar
 * @author Paul Ferraro
 * @version Aug 2014
 */
public enum ThreadPoolResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ThreadPoolResourceRegistration, ResourceServiceConfigurator {

    DEFAULT("default", 0, 200, Duration.ofMinutes(1)),
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private final PathElement path;
    private final RuntimeCapability<Void> capability;

    private final AttributeDefinition minThreads;
    private final AttributeDefinition maxThreads;
    private final DurationAttributeDefinition keepAlive;

    ThreadPoolResourceDefinitionRegistrar(String name, int defaultMinThreads, int defaultMaxThreads, Duration defaultKeepAlive) {
        this.path = pathElement(name);
        this.capability = RuntimeCapability.Builder.of(this).setDynamicNameMapper(UnaryCapabilityNameResolver.GRANDPARENT).build();
        this.minThreads = createAttribute("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), IntRangeValidator.NON_NEGATIVE);
        this.maxThreads = createAttribute("max-threads", ModelType.INT, new ModelNode(defaultMaxThreads), IntRangeValidator.NON_NEGATIVE);
        this.keepAlive = DurationAttributeDefinition.builder("keepalive-time", ChronoUnit.MILLIS).setDefaultValue(defaultKeepAlive).build();
    }

    private static AttributeDefinition createAttribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setValidator(validator)
                .build();
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.path, WILDCARD_PATH);
        ResourceDescriptor descriptor = ResourceDescriptor.builder(resolver)
                .addAttributes(List.of(this.getMinThreads(), this.getMaxThreads(), this.getKeepAlive()))
                .addCapability(this.capability)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();

        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this, resolver).build());

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        int minThreads = this.minThreads.resolveModelAttribute(context, model).asInt();
        int maxThreads = this.maxThreads.resolveModelAttribute(context, model).asInt();
        Duration keepAlive = this.keepAlive.resolve(context, model);
        ThreadPoolConfiguration configuration = new ThreadPoolConfiguration() {
            @Override
            public int getMinThreads() {
                return minThreads;
            }

            @Override
            public int getMaxThreads() {
                return maxThreads;
            }

            @Override
            public Duration getKeepAlive() {
                return keepAlive;
            }
        };
        return CapabilityServiceInstaller.builder(this.capability, configuration).build();
    }

    @Override
    public AttributeDefinition getMinThreads() {
        return this.minThreads;
    }

    @Override
    public AttributeDefinition getMaxThreads() {
        return this.maxThreads;
    }

    @Override
    public DurationAttributeDefinition getKeepAlive() {
        return this.keepAlive;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}