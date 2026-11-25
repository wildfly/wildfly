/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Function;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * @author Paul Ferraro
 *
 */
public enum SocketProtocolResourceRegistration implements ResourceRegistration, Function<ResourceOperationRuntimeHandler, ChildResourceDefinitionRegistrar> {
    FD_SOCK() {
        @Override
        public ChildResourceDefinitionRegistrar apply(ResourceOperationRuntimeHandler runtimeHandler) {
            return new LegacyFailureDetectionProtocolResourceDefinitionRegistrar(this, runtimeHandler);
        }
    },
    FD_SOCK2() {
        @Override
        public ChildResourceDefinitionRegistrar apply(ResourceOperationRuntimeHandler runtimeHandler) {
            return new FailureDetectionProtocolResourceDefinitionRegistrar(this, runtimeHandler);
        }
    },
    ;

    private final PathElement path = StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement(this.name());

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
