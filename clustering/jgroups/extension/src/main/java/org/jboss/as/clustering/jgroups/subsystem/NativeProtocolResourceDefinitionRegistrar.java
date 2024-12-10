/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * Registers a resource definition for a "native" JGroups protocol, identified by its fully qualified name.
 * @author Paul Ferraro
 */
public class NativeProtocolResourceDefinitionRegistrar<P extends Protocol> extends AbstractProtocolResourceDefinitionRegistrar<P> {

    NativeProtocolResourceDefinitionRegistrar(ProtocolResourceDescription description, JGroupsSubsystemModel deprecation, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new ProtocolResourceDescriptorConfigurator<>() {
            @Override
            public ProtocolResourceDescription getResourceDescription() {
                return ProtocolResourceDescription.legacy(description);
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
