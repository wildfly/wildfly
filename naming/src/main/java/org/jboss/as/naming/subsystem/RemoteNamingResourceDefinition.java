/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for JNDI bindings
 */
public class RemoteNamingResourceDefinition extends SimpleResourceDefinition {

    private static final String JBOSS_REMOTING = "org.jboss.remoting";
    static final String REMOTING_ENDPOINT_CAPABILITY_NAME = "org.wildfly.remoting.endpoint";

    public static final RuntimeCapability<Void> REMOTE_NAMING_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.naming.remote")
            .addRequirements(REMOTING_ENDPOINT_CAPABILITY_NAME)
            .build();

    RemoteNamingResourceDefinition() {
        super(new Parameters(NamingSubsystemModel.REMOTE_NAMING_PATH, NamingExtension.getResourceDescriptionResolver(NamingSubsystemModel.REMOTE_NAMING))
                .setAddHandler(RemoteNamingAdd.INSTANCE)
                .setRemoveHandler(RemoteNamingRemove.INSTANCE)
                .addCapabilities(REMOTE_NAMING_CAPABILITY));
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required(JBOSS_REMOTING));
    }
}
