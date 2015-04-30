/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import java.io.Serializable;

import org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsExtension;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.StackResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.TransportResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AdditionalInitialization;

/**
 * Initializer for the JGroups subsystem.
 * @author Paul Ferraro
 */
public class JGroupsSubsystemInitialization extends AdditionalInitialization implements Serializable {
    private static final long serialVersionUID = -4433079373360352449L;
    private final String module = "jgroups";
    private final RunningMode mode;

    public JGroupsSubsystemInitialization() {
        this(RunningMode.ADMIN_ONLY);
    }

    public JGroupsSubsystemInitialization(RunningMode mode) {
        this.mode = mode;
    }

    @Override
    protected RunningMode getRunningMode() {
        return this.mode;
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        new JGroupsExtension().initialize(registry.getExtensionContext(this.module, registration, ExtensionRegistryType.MASTER));

        Resource subsystem = Resource.Factory.create();
        subsystem.getModel().get(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.getName()).set("tcp");
        root.registerChild(JGroupsSubsystemResourceDefinition.PATH, subsystem);

        Resource channel = Resource.Factory.create();
        subsystem.registerChild(ChannelResourceDefinition.pathElement("maximal-channel"), channel);

        Resource stack = Resource.Factory.create();
        subsystem.registerChild(StackResourceDefinition.pathElement("tcp"), stack);

        Resource transport = Resource.Factory.create();
        stack.registerChild(TransportResourceDefinition.pathElement("TCP"), transport);
    }
}