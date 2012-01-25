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

import java.util.Locale;
import java.util.ResourceBundle;

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
        ModelNode description = createDescription(resources, "jgroups");
        description.get(ModelDescriptionConstants.HEAD_COMMENT_ALLOWED).set(true);
        description.get(ModelDescriptionConstants.TAIL_COMMENT_ALLOWED).set(true);
        description.get(ModelDescriptionConstants.NAMESPACE).set(Namespace.CURRENT.getUri());
        return description;
    }

    static ModelNode getSubsystemAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createSubsystemOperationDescription(ModelDescriptionConstants.ADD, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_STACK, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_STACK, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("jgroups.default-stack"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_STACK, ModelDescriptionConstants.REQUIRED).set(false);
        return description;
    }

    static ModelNode getSubsystemRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createSubsystemOperationDescription(ModelDescriptionConstants.ADD, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    static ModelNode getSubsystemDescribeDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createSubsystemOperationDescription(ModelDescriptionConstants.DESCRIBE, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES).setEmptyObject();
        description.get(ModelDescriptionConstants.REPLY_PROPERTIES, ModelDescriptionConstants.TYPE).set(ModelType.LIST);
        description.get(ModelDescriptionConstants.REPLY_PROPERTIES, ModelDescriptionConstants.VALUE_TYPE).set(ModelType.OBJECT);
        return description;
    }

    static ModelNode getProtocolStackDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        return createDescription(resources, "jgroups.stack");
    }

    static ModelNode getProtocolStackAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createProtocolStackOperationDescription(ModelDescriptionConstants.ADD, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.TRANSPORT, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.TRANSPORT, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("jgroups.stack.transport"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.PROTOCOLS, ModelDescriptionConstants.TYPE).set(ModelType.LIST);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.PROTOCOLS, ModelDescriptionConstants.VALUE_TYPE).set(ModelType.OBJECT);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.PROTOCOLS, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("jgroups.stack.protocol"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.PROTOCOLS, ModelDescriptionConstants.REQUIRED).set(false);
        return description;
    }

    static ModelNode getProtocolStackRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createProtocolStackOperationDescription(ModelDescriptionConstants.REMOVE, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES).setEmptyObject();
        return description;
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

    private static ModelNode createSubsystemOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "jgroups." + operation);
    }

    private static ModelNode createProtocolStackOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "jgroups.stack." + operation);
    }
}
