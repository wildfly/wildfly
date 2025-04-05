/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.ResourceRegistration;
import org.jgroups.Global;
import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * Registers a resource definition for a "native" JGroups protocol, identified by its fully qualified name.
 * @author Paul Ferraro
 */
public class NativeProtocolResourceDefinitionRegistrar<P extends Protocol> extends AbstractProtocolResourceDefinitionRegistrar<P> {

    NativeProtocolResourceDefinitionRegistrar(ResourceRegistration registration, JGroupsSubsystemModel deprecation, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return ResourceRegistration.of(StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement(Global.PREFIX + registration.getPathElement().getValue()));
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
