/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.ModuleAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.clustering.controller.StatisticsEnabledAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.ChannelConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;

/**
 * Description of a channel resource.
 * @author Paul Ferraro
 */
public enum ChannelResourceDescription implements ResourceDescription {
    INSTANCE;

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("channel", name);
    }

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(ChannelConfiguration.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    static final CapabilityReferenceAttributeDefinition<ChannelFactory> STACK = new CapabilityReferenceAttributeDefinition.Builder<>("stack", CapabilityReference.builder(CAPABILITY, ChannelFactory.SERVICE_DESCRIPTOR).build()).build();
    static final ModuleAttributeDefinition MODULE = new ModuleAttributeDefinition.Builder().setRequired(false).setDefaultValue(new ModelNode("org.wildfly.clustering.server")).build();
    static final AttributeDefinition CLUSTER = new SimpleAttributeDefinitionBuilder("cluster", ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();
    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().build();

    private final PathElement path = pathElement(PathElement.WILDCARD_VALUE);

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.of(STACK, MODULE, CLUSTER, STATISTICS_ENABLED);
    }
}
