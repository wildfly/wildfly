/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.extension.clustering.web.routing.LocalRoutingProvider;

/**
 * Registers a resource definition for a local routing provider.
 * @author Paul Ferraro
 */
public class LocalRoutingProviderResourceDefinitionRegistrar extends RoutingProviderResourceDefinitionRegistrar {

    LocalRoutingProviderResourceDefinitionRegistrar() {
        super(RoutingProviderResourceRegistration.LOCAL);
    }

    @Override
    public RoutingProvider resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return new LocalRoutingProvider();
    }
}
