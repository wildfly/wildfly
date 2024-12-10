/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.clustering.jgroups.spi.ChannelConfiguration;
import org.wildfly.clustering.jgroups.spi.RemoteSiteConfiguration;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;

/**
 * @author Paul Ferraro
 *
 */
public enum RemoteSiteResourceDescription implements ResourceDescription {
    INSTANCE;

    private final PathElement path = PathElement.pathElement("remote-site");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(RemoteSiteConfiguration.SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_CHILD).build();

    static final CapabilityReferenceAttributeDefinition<ChannelConfiguration> CHANNEL_CONFIGURATION = new CapabilityReferenceAttributeDefinition.Builder<>("channel", CapabilityReference.builder(CAPABILITY, ChannelConfiguration.SERVICE_DESCRIPTOR).build()).build();

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.of(CHANNEL_CONFIGURATION);
    }
}
