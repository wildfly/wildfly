/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * @author Paul Ferraro
 *
 */
public class DataSourcesSubsystemInitialization extends AdditionalInitialization {
    private static final long serialVersionUID = -8991959025968193007L;

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        Extension extension = new DataSourcesExtension();
        extension.initialize(registry.getExtensionContext("test", registration, ExtensionRegistryType.MASTER));

        Resource dataSourcesSubsystem = Resource.Factory.create();
        root.registerChild(PathElement.pathElement(SUBSYSTEM, DataSourcesExtension.SUBSYSTEM_NAME), dataSourcesSubsystem);

        Resource dataSource = Resource.Factory.create();
        dataSourcesSubsystem.registerChild(PathElement.pathElement("data-source", "ExampleDS"), dataSource);
        dataSource.getModel().get("jndi-name").set("java:jboss/jdbc/store");

        super.initializeExtraSubystemsAndModel(registry, root, registration, capabilityRegistry);
    }
}
