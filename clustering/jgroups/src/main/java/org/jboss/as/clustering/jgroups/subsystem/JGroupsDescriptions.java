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

import static org.jboss.as.clustering.jgroups.subsystem.CommonAttributes.DEFAULT_STACK;
import static org.jboss.as.clustering.jgroups.subsystem.CommonAttributes.VALUE;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Access the resource bundle used to fetch local descriptions.
 * @author Paul Ferraro
 */
public class JGroupsDescriptions {

    public static final String RESOURCE_NAME = JGroupsDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    private JGroupsDescriptions() {
        // Hide
    }

    static ModelNode getSubsystemDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode subsystem = createDescription(resources, "jgroups");
        subsystem.get(ModelDescriptionConstants.HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(ModelDescriptionConstants.TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(ModelDescriptionConstants.NAMESPACE).set(Namespace.CURRENT.getUri());
        // children of the root subsystem
        subsystem.get(CHILDREN, ModelKeys.STACK, DESCRIPTION).set(resources.getString("jgroups.stack"));
        subsystem.get(CHILDREN, ModelKeys.STACK, MIN_OCCURS).set(1);
        subsystem.get(CHILDREN, ModelKeys.STACK, MAX_OCCURS).set(Integer.MAX_VALUE);
        subsystem.get(CHILDREN, ModelKeys.STACK, MODEL_DESCRIPTION).setEmptyObject();
        return subsystem;
    }

    static ModelNode getSubsystemAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "jgroups.add");
        DEFAULT_STACK.addOperationParameterDescription(resources, "jgroups", op);
        return op;
    }

    static ModelNode getSubsystemRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "jgroups.remove");
        op.get(REPLY_PROPERTIES).setEmptyObject();
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getSubsystemDescribeDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(DESCRIBE, resources, "jgroups.describe");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
        op.get(REPLY_PROPERTIES, VALUE_TYPE).set(ModelType.OBJECT);
        return op;
    }

    static ModelNode getProtocolStackDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode stack = createDescription(resources, "jgroups.stack");

        // information about its child "transport=TRANSPORT"
        stack.get(CHILDREN, ModelKeys.TRANSPORT, DESCRIPTION).set(resources.getString("jgroups.stack.transport"));
        stack.get(CHILDREN, ModelKeys.TRANSPORT, MIN_OCCURS).set(0);
        stack.get(CHILDREN, ModelKeys.TRANSPORT, MAX_OCCURS).set(1);
        stack.get(CHILDREN, ModelKeys.TRANSPORT, ALLOWED).setEmptyList().add(ModelKeys.TRANSPORT_NAME);
        stack.get(CHILDREN, ModelKeys.TRANSPORT, MODEL_DESCRIPTION);

        // information about its child "protocol=*"
        stack.get(CHILDREN, ModelKeys.PROTOCOL, DESCRIPTION).set(resources.getString("jgroups.stack.protocol"));
        stack.get(CHILDREN, ModelKeys.PROTOCOL, MIN_OCCURS).set(0);
        stack.get(CHILDREN, ModelKeys.PROTOCOL, MAX_OCCURS).set(Integer.MAX_VALUE);
        stack.get(CHILDREN, ModelKeys.PROTOCOL, MODEL_DESCRIPTION);

        return stack ;
    }

    static ModelNode getProtocolStackAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);

        final ModelNode op = createOperationDescription(ADD, resources, "jgroups.stack.add");
        return op;
    }

    static ModelNode getProtocolStackRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "jgroups.stack.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    // stack transport element
    static ModelNode getTransportDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode transport = createDescription(resources, "jgroups.stack.transport");
        // attributes for adding a transport
        for (AttributeDefinition attr : CommonAttributes.TRANSPORT_ATTRIBUTES) {
            // don't add PROPERTIES to the resource - just its add operation
            if (attr.getName().equals(CommonAttributes.PROPERTIES.getName()))
                continue ;
            attr.addResourceAttributeDescription(resources, "jgroups.stack.transport", transport);
        }
        // information about its child "property=*"
        transport.get(CHILDREN, ModelKeys.PROPERTY, DESCRIPTION).set(resources.getString("jgroups.stack.transport.property"));
        transport.get(CHILDREN, ModelKeys.PROPERTY, MIN_OCCURS).set(0);
        transport.get(CHILDREN, ModelKeys.PROPERTY, MAX_OCCURS).set(Integer.MAX_VALUE);
        transport.get(CHILDREN, ModelKeys.PROPERTY, MODEL_DESCRIPTION);
        return transport ;
    }

    static ModelNode getTransportAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "jgroups.stack.transport.add");
        for (AttributeDefinition attr : CommonAttributes.TRANSPORT_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "jgroups.stack.transport", op);
        }
        return op;
    }

    static ModelNode getTransportRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "jgroups.stack.transport.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }


    // stack protocol element
    static ModelNode getProtocolDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode protocol = createDescription(resources, "jgroups.stack.protocol");
        // attributes for adding a protocol
        for (AttributeDefinition attr : CommonAttributes.PROTOCOL_ATTRIBUTES) {
            // don't add PROPERTIES to the resource - just its add operation
            if (attr.getName().equals(CommonAttributes.PROPERTIES.getName()))
                continue ;
            attr.addResourceAttributeDescription(resources, "jgroups.stack.protocol", protocol);
        }
        // information about its child "property=*"
        protocol.get(CHILDREN, ModelKeys.PROPERTY, DESCRIPTION).set(resources.getString("jgroups.stack.protocol.property"));
        protocol.get(CHILDREN, ModelKeys.PROPERTY, MIN_OCCURS).set(0);
        protocol.get(CHILDREN, ModelKeys.PROPERTY, MAX_OCCURS).set(Integer.MAX_VALUE);
        protocol.get(CHILDREN, ModelKeys.PROPERTY, MODEL_DESCRIPTION);
        return protocol ;
    }

    static ModelNode getProtocolAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.ADD_PROTOCOL, resources, "jgroups.stack.protocol.add-protocol");
        for (AttributeDefinition attr : CommonAttributes.PROTOCOL_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "jgroups.stack.protocol", op);
        }
        return op;
    }

    static ModelNode getProtocolRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.REMOVE_PROTOCOL, resources, "jgroups.stack.protocol.remove-protocol");
        CommonAttributes.TYPE.addOperationParameterDescription(resources, "jgroups.stack.protocol", op);
        return op;
    }

    // protocol property element
    static ModelNode getProtocolPropertyDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode protocolProperty = createDescription(resources, "jgroups.stack.protocol.property");
        VALUE.addResourceAttributeDescription(resources, "jgroups.stack.protocol.property", protocolProperty);
        return protocolProperty ;
    }

    static ModelNode getProtocolPropertyAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "jgroups.stack.protocol.property.add");
        VALUE.addOperationParameterDescription(resources, "jgroups.stack.protocol.property", op);
        return op;
    }

    static ModelNode getProtocolPropertyRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "jgroups.stack.protocol.property.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getExportNativeConfigurationDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.EXPORT_NATIVE_CONFIGURATION, resources, "jgroups.stack.export-native-configuration");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        return op;
    }


    private static ResourceBundle getResources(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_NAME, (locale == null) ? Locale.getDefault() : locale);
    }

    private static ModelNode createDescription(ResourceBundle resources, String key) {
        return createOperationDescription(null, resources, key);
    }

    private static ModelNode createOperationDescription(String operation, ResourceBundle resources, String key) {
        ModelNode description = new ModelNode();
        if (operation != null) {
            description.get(ModelDescriptionConstants.OPERATION_NAME).set(operation);
        }
        description.get(ModelDescriptionConstants.DESCRIPTION).set(resources.getString(key));
        return description;
    }
}
