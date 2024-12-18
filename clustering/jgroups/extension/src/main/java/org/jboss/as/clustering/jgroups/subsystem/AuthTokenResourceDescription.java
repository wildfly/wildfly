/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CredentialReferenceAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jgroups.auth.AuthToken;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * @author Paul Ferraro
 */
public interface AuthTokenResourceDescription extends ResourceDescription, UnaryOperator<ResourceDescriptor.Builder> {

    static PathElement pathElement(String value) {
        return PathElement.pathElement("token", value);
    }

    static final BinaryServiceDescriptor<AuthToken> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.clustering.jgroups.auth-token", AuthToken.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    static final CredentialReferenceAttributeDefinition SHARED_SECRET = new CredentialReferenceAttributeDefinition.Builder("shared-secret-reference", CAPABILITY).build();

    @Override
    default PathElement getPathKey() {
        return pathElement(PathElement.WILDCARD_VALUE);
    }

    @Override
    default Stream<AttributeDefinition> getAttributes() {
        return Stream.of(SHARED_SECRET);
    }

    @Override
    default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addCapability(CAPABILITY).addAttribute(SHARED_SECRET, CredentialReferenceWriteAttributeHandler.INSTANCE);
    }
}
