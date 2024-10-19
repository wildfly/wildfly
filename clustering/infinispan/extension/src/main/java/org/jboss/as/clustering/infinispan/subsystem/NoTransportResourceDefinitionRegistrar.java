/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for the non-clustered transport of a cache container.
 * @author Paul Ferraro
 */
public class NoTransportResourceDefinitionRegistrar extends TransportResourceDefinitionRegistrar {

    NoTransportResourceDefinitionRegistrar() {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return TransportResourceRegistration.NONE;
            }

            @Override
            public String resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                return ModelDescriptionConstants.LOCAL;
            }
        });
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
}
