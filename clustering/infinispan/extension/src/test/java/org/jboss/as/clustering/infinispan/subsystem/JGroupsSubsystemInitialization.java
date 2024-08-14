/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.jgroups.subsystem.JGroupsExtension;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsResourceRegistration;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinitionRegistrar;
import org.jboss.as.clustering.jgroups.subsystem.StackResourceDefinitionRegistrar;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * @author Paul Ferraro
 *
 */
public class JGroupsSubsystemInitialization extends AdditionalInitialization {
    private static final long serialVersionUID = -8991959025968193007L;

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        Extension extension = new JGroupsExtension();
        extension.initialize(registry.getExtensionContext("test", extension.getStability(), registration, ExtensionRegistryType.MASTER));

        Resource jgroupsSubsystem = Resource.Factory.create();
        // Need to use explicit names here due to signature change ("NoSuchMethodError: org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition$Attribute.getName()Ljava/lang/String;")
        jgroupsSubsystem.getModel().get("default-stack").set("tcp");
        jgroupsSubsystem.getModel().get("default-channel").set("maximal-channel");
        root.registerChild(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement(), jgroupsSubsystem);

        Resource channel = Resource.Factory.create();
        jgroupsSubsystem.registerChild(JGroupsResourceRegistration.CHANNEL.pathElement("maximal-channel"), channel);

        Resource stack = Resource.Factory.create();
        jgroupsSubsystem.registerChild(JGroupsResourceRegistration.STACK.pathElement("tcp"), stack);

        Resource transport = Resource.Factory.create();
        stack.registerChild(StackResourceDefinitionRegistrar.Component.TRANSPORT.pathElement("TCP"), transport);

        super.initializeExtraSubystemsAndModel(registry, root, registration, capabilityRegistry);
    }
}
