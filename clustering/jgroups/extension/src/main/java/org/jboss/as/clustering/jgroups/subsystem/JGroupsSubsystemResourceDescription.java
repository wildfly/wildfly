/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.SubsystemResourceDescription;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.JGroupsServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;

/**
 * @author Paul Ferraro
 *
 */
public enum JGroupsSubsystemResourceDescription implements SubsystemResourceDescription {
    INSTANCE;

    static final RuntimeCapability<Void> DEFAULT_CHANNEL_CAPABILITY = RuntimeCapability.Builder.of(JGroupsServiceDescriptor.DEFAULT_CHANNEL).build();

    static final CapabilityReferenceAttributeDefinition<JChannel> DEFAULT_CHANNEL = new CapabilityReferenceAttributeDefinition.Builder<>("default-channel", CapabilityReference.builder(DEFAULT_CHANNEL_CAPABILITY, JGroupsServiceDescriptor.CHANNEL).build())
            .setXmlName(ModelDescriptionConstants.DEFAULT)
            .setAttributeGroup("channels")
            .setRequired(false)
            .build();

    @Override
    public String getName() {
        return "jgroups";
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.of(DEFAULT_CHANNEL);
    }
}
