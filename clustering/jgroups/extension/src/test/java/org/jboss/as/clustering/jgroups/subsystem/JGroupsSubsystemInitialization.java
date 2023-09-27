/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * Initializer for the JGroups subsystem.
 * @author Paul Ferraro
 */
public class JGroupsSubsystemInitialization extends AdditionalInitialization {
    private static final long serialVersionUID = -4433079373360352449L;

    public JGroupsSubsystemInitialization() {
        super();
    }

    public JGroupsSubsystemInitialization(RunningMode mode) {
        super(mode);
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        new JGroupsExtension().initialize(registry.getExtensionContext("jgroups", registration, ExtensionRegistryType.MASTER));

        Resource subsystem = Resource.Factory.create();
        // Need to use explicit names here due to signature change ("NoSuchMethodError: org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition$Attribute.getName()Ljava/lang/String;")
        subsystem.getModel().get("default-stack").set("tcp");
        subsystem.getModel().get("default-channel").set("maximal-channel");
        root.registerChild(JGroupsSubsystemResourceDefinition.PATH, subsystem);

        Resource channel = Resource.Factory.create();
        subsystem.registerChild(ChannelResourceDefinition.pathElement("maximal-channel"), channel);

        Resource stack = Resource.Factory.create();
        subsystem.registerChild(StackResourceDefinition.pathElement("tcp"), stack);

        Resource transport = Resource.Factory.create();
        stack.registerChild(TransportResourceDefinition.pathElement("TCP"), transport);

        super.initializeExtraSubystemsAndModel(registry, root, registration, capabilityRegistry);
    }
}