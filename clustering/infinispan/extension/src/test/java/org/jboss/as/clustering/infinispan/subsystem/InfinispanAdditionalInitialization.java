/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.io.Serializable;

import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemInitialization;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * @author Radoslav Husar
 */
public class InfinispanAdditionalInitialization extends JGroupsSubsystemInitialization implements Serializable {

    private static final long serialVersionUID = 6976221515049410278L;

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        super.initializeExtraSubystemsAndModel(registry, root, registration, capabilityRegistry);

        initializeDatasources(registry, root, registration, capabilityRegistry);
    }

    private void initializeDatasources(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        // Needed to test org.jboss.as.clustering.infinispan.subsystem.JDBCStoreResourceDefinition.DeprecatedAttribute.DATASOURCE conversion
        new DataSourcesExtension().initialize(registry.getExtensionContext("datasources", registration, ExtensionRegistryType.MASTER));

        Resource subsystem = Resource.Factory.create();
        PathElement path = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, DataSourcesExtension.SUBSYSTEM_NAME);
        root.registerChild(path, subsystem);

        Resource dataSource = Resource.Factory.create();
        dataSource.getModel().get("jndi-name").set("java:jboss/jdbc/store");
        subsystem.registerChild(PathElement.pathElement("data-source", "ExampleDS"), dataSource);
    }
}