/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Function;

import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a plain authentication token for use by the AUTH protocol.
 * @author Paul Ferraro
 */
public class PlainAuthTokenResourceDefinitionRegistrar extends AuthTokenResourceDefinitionRegistrar<BinaryAuthToken> {

    PlainAuthTokenResourceDefinitionRegistrar() {
        super(Token.PLAIN);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder;
    }

    @Override
    public ServiceDependency<Function<byte[], BinaryAuthToken>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return ServiceDependency.of(BinaryAuthToken::new);
    }
}
