/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.service;

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.ResourceModelResolver;

/**
 * Encapsulates the attributes that describe a cache configuration.
 * @author Paul Ferraro
 */
public interface CacheConfigurationAttributeGroup extends ResourceModelResolver<BinaryServiceConfiguration> {

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

    /**
     * Convenience method returning a collection containing the container and cache attributes.
     * @return a collection of attribute definitions.
     */
    default Collection<AttributeDefinition> getAttributes() {
        return List.of(this.getContainerAttribute(), this.getCacheAttribute());
    }

    @Override
    default BinaryServiceConfiguration resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String containerName = this.getContainerAttribute().resolveModelAttribute(context, model).asString();
        String cacheName = this.getCacheAttribute().resolveModelAttribute(context, model).asStringOrNull();
        return BinaryServiceConfiguration.of(containerName, cacheName);
    }
}
