/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathElement;
import org.jgroups.Global;
import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * Registers a resource definition for a "native" JGroups protocol, identified by its fully qualified name.
 * @author Paul Ferraro
 */
public class NativeProtocolResourceDefinitionRegistrar<P extends Protocol> extends AbstractProtocolResourceDefinitionRegistrar<P> {

    public static PathElement pathElement(String name) {
        return AbstractProtocolResourceDefinitionRegistrar.pathElement(Global.PREFIX + name);
    }

    NativeProtocolResourceDefinitionRegistrar(String name, JGroupsSubsystemModel deprecation, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new ProtocolResourceRegistration<>() {
            @Override
            public PathElement getPathElement() {
                return pathElement(name);
            }

            @Override
            public JGroupsSubsystemModel getDeprecation() {
                return deprecation;
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }
        });
    }
}
