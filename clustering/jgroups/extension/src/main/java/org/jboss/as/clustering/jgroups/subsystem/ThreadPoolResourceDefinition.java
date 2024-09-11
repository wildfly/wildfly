/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Radoslav Husar
 * @author Paul Ferraro
 * @version Aug 2014
 */
public enum ThreadPoolResourceDefinition implements ResourceDefinitionProvider, ThreadPoolDefinition, ThreadPoolServiceDescriptor, ResourceServiceConfigurator {

    DEFAULT("default", 0, 200, 0, 60000L),
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private final PathElement path;
    private final RuntimeCapability<Void> capability;
    private final Attribute minThreads;
    private final Attribute maxThreads;
    private final Attribute keepAliveTime;

    ThreadPoolResourceDefinition(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepAliveTime) {
        this.path = pathElement(name);
        this.minThreads = new SimpleAttribute(createAttribute("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), IntRangeValidator.NON_NEGATIVE));
        this.maxThreads = new SimpleAttribute(createAttribute("max-threads", ModelType.INT, new ModelNode(defaultMaxThreads), IntRangeValidator.NON_NEGATIVE));
        this.keepAliveTime = new SimpleAttribute(createAttribute("keepalive-time", ModelType.LONG, new ModelNode(defaultKeepAliveTime), LongRangeValidator.NON_NEGATIVE));
        this.capability = RuntimeCapability.Builder.of(this).setAllowMultipleRegistrations(true).setDynamicNameMapper(UnaryCapabilityNameResolver.GRANDPARENT).build();
    }

    private static AttributeDefinition createAttribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                .setValidator(validator)
                .build();
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ResourceDescriptionResolver resolver = JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(this.path, pathElement(PathElement.WILDCARD_VALUE));
        SimpleResourceDefinition.Parameters parameters = new SimpleResourceDefinition.Parameters(this.path, resolver);
        ResourceDefinition definition = new SimpleResourceDefinition(parameters);
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(definition);

        ResourceDescriptor descriptor = new ResourceDescriptor(resolver)
                .addAttributes(this.minThreads, this.maxThreads, this.keepAliveTime)
                .addCapabilities(List.of(this.capability))
                ;
        ResourceServiceHandler handler = ResourceServiceHandler.of(ResourceOperationRuntimeHandler.configureService(this));
        new SimpleResourceRegistrar(descriptor, handler).register(registration);
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        int minThreads = this.minThreads.resolveModelAttribute(context, model).asInt();
        int maxThreads = this.maxThreads.resolveModelAttribute(context, model).asInt();
        long keepAliveTime = this.keepAliveTime.resolveModelAttribute(context, model).asLong();
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
            public long getKeepAliveTime() {
                return keepAliveTime;
            }
        };
        return CapabilityServiceInstaller.builder(this.capability, configuration).build();
    }

    Collection<Attribute> getAttributes() {
        return Arrays.asList(this.minThreads, this.maxThreads, this.keepAliveTime);
    }

    @Override
    public Attribute getMinThreads() {
        return this.minThreads;
    }

    @Override
    public Attribute getMaxThreads() {
        return this.maxThreads;
    }

    @Override
    public Attribute getKeepAliveTime() {
        return this.keepAliveTime;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}