/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes an non-persistent persistence cache component resource.
 * @author Paul Ferraro
 */
public enum NoStoreResourceDescription implements PersistenceResourceDescription {
    INSTANCE;

    private final PathElement path = PersistenceResourceDescription.pathElement("none");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public ServiceDependency<PersistenceConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return ServiceDependency.from(ConfigurationBuilder::new).map(ConfigurationBuilder::persistence);
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.empty();
    }
}
