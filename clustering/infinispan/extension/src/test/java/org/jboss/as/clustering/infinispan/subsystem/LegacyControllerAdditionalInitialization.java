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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;


import org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsExtension;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.StackResourceDefinition;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * Copy of {@link org.jboss.as.clustering.subsystem.AdditionalInitialization} to workaround classloading issues on the legacy controller.
 *
 * @author Radoslav Husar
 */
class LegacyControllerAdditionalInitialization extends AdditionalInitialization implements Serializable {
    private static final long serialVersionUID = -9114460471733548707L;

    private final List<String> requirements = new LinkedList<>();

    @Override
    protected RunningMode getRunningMode() {
        return RunningMode.ADMIN_ONLY;
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        initializeDatasources(registry, root, registration, capabilityRegistry);
        initializeJGroups(registry, root, registration, capabilityRegistry);

        registerCapabilities(capabilityRegistry, this.requirements.stream().toArray(String[]::new));
    }

    public LegacyControllerAdditionalInitialization require(Requirement requirement) {
        this.requirements.add(requirement.getName());
        return this;
    }

    public LegacyControllerAdditionalInitialization require(UnaryRequirement requirement, String... names) {
        Stream.of(names).forEach(name -> this.requirements.add(requirement.resolve(name)));
        return this;
    }

    // Moved from org.jboss.as.clustering.infinispan.subsystem.TransformersTestCase#createAdditionalInitialization()
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

    // Copied with changes from org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemInitialization#initializeExtraSubystemsAndModel()
    private void initializeJGroups(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        new JGroupsExtension().initialize(registry.getExtensionContext("jgroups", registration, ExtensionRegistryType.MASTER));

        Resource subsystem = Resource.Factory.create();
        // Changed to reference these via names, otherwise fail with org.jboss.msc.service.StartException in service jboss.as.server-controller: java.lang.NoSuchMethodError: org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition$Attribute.getName()Ljava/lang/String;
        subsystem.getModel().get("default-stack").set("tcp");
        subsystem.getModel().get("default-channel").set("maximal-channel");
        root.registerChild(JGroupsSubsystemResourceDefinition.PATH, subsystem);

        Resource channel = Resource.Factory.create();
        subsystem.registerChild(ChannelResourceDefinition.pathElement("maximal-channel"), channel);

        Resource stack = Resource.Factory.create();
        subsystem.registerChild(StackResourceDefinition.pathElement("tcp"), stack);

        Resource transport = Resource.Factory.create();
        stack.registerChild(org.jboss.as.clustering.jgroups.subsystem.TransportResourceDefinition.pathElement("TCP"), transport);
    }

}