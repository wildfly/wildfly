/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
