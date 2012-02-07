/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Registers the JGroups subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JGroupsExtension implements Extension {

    static final String SUBSYSTEM_NAME = "jgroups";

    private static final PathElement stacksPath = PathElement.pathElement(ModelKeys.STACK);
    private static final PathElement transportPath = PathElement.pathElement(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
    private static final PathElement protocolPath = PathElement.pathElement(ModelKeys.PROTOCOL);
    private static final PathElement protocolPropertyPath = PathElement.pathElement(ModelKeys.PROPERTY);

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, Namespace.CURRENT.getMajorVersion(), Namespace.CURRENT.getMinorVersion());
        subsystem.registerXMLElementWriter(new JGroupsSubsystemXMLWriter());

        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(JGroupsSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, JGroupsSubsystemAdd.INSTANCE, JGroupsSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(ModelDescriptionConstants.REMOVE, JGroupsSubsystemRemove.INSTANCE, JGroupsSubsystemProviders.SUBSYSTEM_REMOVE, false);
        registration.registerOperationHandler(ModelDescriptionConstants.DESCRIBE, JGroupsSubsystemDescribe.INSTANCE, JGroupsSubsystemProviders.SUBSYSTEM_DESCRIBE, false, EntryType.PRIVATE);

        ManagementResourceRegistration stacks = registration.registerSubModel(stacksPath, JGroupsSubsystemProviders.STACK);
        stacks.registerOperationHandler(ModelDescriptionConstants.ADD, ProtocolStackAdd.INSTANCE, JGroupsSubsystemProviders.STACK_ADD, false);
        stacks.registerOperationHandler(ModelDescriptionConstants.REMOVE, ProtocolStackRemove.INSTANCE, JGroupsSubsystemProviders.STACK_REMOVE, false);
        // register the add-protocol and remove-protocol handlers
        stacks.registerOperationHandler(ModelKeys.ADD_PROTOCOL, StackConfigOperationHandlers.PROTOCOL_ADD, JGroupsSubsystemProviders.PROTOCOL_ADD);
        stacks.registerOperationHandler(ModelKeys.REMOVE_PROTOCOL, StackConfigOperationHandlers.PROTOCOL_REMOVE, JGroupsSubsystemProviders.PROTOCOL_REMOVE);
        // register the export operation
        if (context.isRuntimeOnlyRegistrationValid()) {
            final EnumSet<OperationEntry.Flag> readOnly = EnumSet.of(OperationEntry.Flag.READ_ONLY);
            stacks.registerOperationHandler(ModelKeys.EXPORT_NATIVE_CONFIGURATION, StackConfigOperationHandlers.EXPORT_NATIVE_CONFIGURATION, JGroupsSubsystemProviders.EXPORT_NATIVE_CONFIGURATION, readOnly);
        }

        // register the transport=TRANSPORT handlers
        final ManagementResourceRegistration transport = stacks.registerSubModel(transportPath, JGroupsSubsystemProviders.TRANSPORT);
        transport.registerOperationHandler(ADD, StackConfigOperationHandlers.TRANSPORT_ADD, JGroupsSubsystemProviders.TRANSPORT_ADD);
        transport.registerOperationHandler(REMOVE, StackConfigOperationHandlers.REMOVE, JGroupsSubsystemProviders.TRANSPORT_REMOVE);
        StackConfigOperationHandlers.TRANSPORT_ATTR.registerAttributes(transport);
        createPropertyRegistration(transport);

        // register the protocol=* handlers
        final ManagementResourceRegistration protocol = stacks.registerSubModel(protocolPath, JGroupsSubsystemProviders.PROTOCOL);
        StackConfigOperationHandlers.PROTOCOL_ATTR.registerAttributes(protocol);
        JGroupsExtension.createPropertyRegistration(protocol);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace: Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), reader);
            }
        }
    }


    static void createPropertyRegistration(final ManagementResourceRegistration parent) {
        final ManagementResourceRegistration registration = parent.registerSubModel(protocolPropertyPath, JGroupsSubsystemProviders.PROTOCOL_PROPERTY);
        registration.registerOperationHandler(ADD, StackConfigOperationHandlers.PROTOCOL_PROPERTY_ADD, JGroupsSubsystemProviders.PROTOCOL_PROPERTY_ADD);
        registration.registerOperationHandler(REMOVE, StackConfigOperationHandlers.REMOVE, JGroupsSubsystemProviders.PROTOCOL_PROPERTY_REMOVE);
        registration.registerReadWriteAttribute("value", null, StackConfigOperationHandlers.PROTOCOL_PROPERTY_ATTR, EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES));
    }

}
