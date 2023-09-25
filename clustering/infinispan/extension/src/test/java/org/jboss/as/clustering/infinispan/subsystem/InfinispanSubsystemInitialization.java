/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemInitialization;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * @author Paul Ferraro
 */
public class InfinispanSubsystemInitialization extends JGroupsSubsystemInitialization {
    private static final long serialVersionUID = -8991959025968193007L;

    public InfinispanSubsystemInitialization() {
        super();
    }

    public InfinispanSubsystemInitialization(RunningMode mode) {
        super(mode);
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        new DataSourcesExtension().initialize(registry.getExtensionContext("datasources", registration, ExtensionRegistryType.MASTER));

        Resource subsystem = Resource.Factory.create();
        root.registerChild(PathElement.pathElement(SUBSYSTEM, DataSourcesExtension.SUBSYSTEM_NAME), subsystem);

        Resource dataSource = Resource.Factory.create();
        subsystem.registerChild(PathElement.pathElement("data-source", "ExampleDS"), dataSource);
        dataSource.getModel().get("jndi-name").set("java:jboss/jdbc/store");

        super.initializeExtraSubystemsAndModel(registry, root, registration, capabilityRegistry);
    }
}
