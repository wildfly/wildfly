/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.service;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.AttributeDefinition;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;

/**
 * Encapsulates the attributes that describe a cache configuration.
 * @author Paul Ferraro
 */
public interface CacheConfigurationDescriptor extends UnaryOperator<ResourceDescriptor.Builder> {

    /**
     * Returns the attribute definition for the container attribute.
     * @return an attribute definition
     */
    AttributeDefinition getContainerAttribute();

    /**
     * Returns the attribute definition for the cache attribute.
     * @return an attribute definition
     */
    AttributeDefinition getCacheAttribute();

    default Collection<AttributeDefinition> getAttributes() {
        return List.of(this.getContainerAttribute(), this.getCacheAttribute());
    }

    default ResourceModelResolver<BinaryServiceConfiguration> getResolver() {
        return BinaryServiceConfiguration.resolver(this.getContainerAttribute(), this.getCacheAttribute());
    }

    @Override
    default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addAttributes(List.of(this.getContainerAttribute(), this.getCacheAttribute()));
    }
}
