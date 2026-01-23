/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.DurationAttributeDefinition;

/**
 * Enumerates scheduled thread pool resource registrations for a cache container.
 * @author Radoslav Husar
 */
public enum ScheduledThreadPool implements ScheduledThreadPoolResourceRegistration<ThreadPoolConfiguration> {

    EXPIRATION("expiration", 1, Duration.ofMinutes(1)), // called eviction prior to Infinispan 8
    ;

    private final PathElement path;
    private final UnaryServiceDescriptor<ThreadPoolConfiguration> descriptor;
    private final RuntimeCapability<Void> capability;
    private final AttributeDefinition minThreads;
    private final DurationAttributeDefinition keepAlive;

    ScheduledThreadPool(String name, int defaultMinThreads, Duration defaultKeepAlive) {
        this.path = PathElement.pathElement("thread-pool", name);
        this.descriptor = UnaryServiceDescriptorFactory.createServiceDescriptor(this, ThreadPoolConfiguration.class);
        this.capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();
        this.minThreads = createAttribute("min-threads", defaultMinThreads);
        this.keepAlive = DurationAttributeDefinition.builder("keepalive-time", ChronoUnit.MILLIS).setDefaultValue(defaultKeepAlive).build();
    }

    private static AttributeDefinition createAttribute(String name, int defaultValue) {
        return new SimpleAttributeDefinitionBuilder(name, ModelType.INT)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(new ModelNode(defaultValue))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setValidator(new IntRangeValidator(0))
                .build();
    }

    @Override
    public AttributeDefinition getMinThreads() {
        return this.minThreads;
    }

    @Override
    public DurationAttributeDefinition getKeepAlive() {
        return this.keepAlive;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public UnaryServiceDescriptor<ThreadPoolConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
    }
}