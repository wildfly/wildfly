/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;
import java.util.stream.Stream;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public enum NoTransportResourceDescription implements TransportResourceDescription {
    INSTANCE;

    private final PathElement path = TransportResourceDescription.pathElement("none");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public String resolveGroupName(OperationContext context, ModelNode model) throws OperationFailedException {
        return ModelDescriptionConstants.LOCAL;
    }

    @Override
    public ServiceDependency<TransportConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR).map(new Function<>() {
            @Override
            public TransportConfigurationBuilder apply(ServerEnvironment environment) {
                return new GlobalConfigurationBuilder().transport().transport(null)
                        .clusterName(ModelDescriptionConstants.LOCAL)
                        .nodeName(environment.getNodeName())
                        ;
            }
        });
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.empty();
    }
}
