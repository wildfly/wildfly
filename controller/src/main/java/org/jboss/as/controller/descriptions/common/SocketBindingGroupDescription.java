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
package org.jboss.as.controller.descriptions.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.operations.common.SocketBindingGroupIncludeAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingGroupIncludeRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model descriptions for socket-binding-group and socket-binding elements.
 *
 * @author Brian Stansberry
 */
public class SocketBindingGroupDescription {

    private static final String RESOURCE_NAME = SocketBindingGroupDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getServerSocketBindingGroupDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = getGroupDescription(bundle.getString("socket_binding_group"), bundle);
        root.get(ATTRIBUTES, PORT_OFFSET, TYPE).set(ModelType.INT);
        root.get(ATTRIBUTES, PORT_OFFSET, DESCRIPTION).set(bundle.getString("server_socket_binding_group.port-offset"));
        root.get(ATTRIBUTES, PORT_OFFSET, REQUIRED).set(false);
        root.get(ATTRIBUTES, PORT_OFFSET, HEAD_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, PORT_OFFSET, TAIL_COMMENT_ALLOWED).set(false);
        return root;
    }

    public static ModelNode getDomainSocketBindingGroupDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = getGroupDescription(bundle.getString("socket_binding_group"), bundle);
        root.get(ATTRIBUTES, INCLUDES, TYPE).set(ModelType.LIST);
        root.get(ATTRIBUTES, INCLUDES, VALUE_TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, INCLUDES, DESCRIPTION).set(bundle.getString("domain_socket_binding_group.includes"));
        root.get(ATTRIBUTES, INCLUDES, REQUIRED).set(false);
        root.get(ATTRIBUTES, INCLUDES, HEAD_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, INCLUDES, TAIL_COMMENT_ALLOWED).set(false);
        return root;
    }

    private static ModelNode getGroupDescription(String description, ResourceBundle bundle) {
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(description);
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("socket_binding_group.name"));
        root.get(ATTRIBUTES, NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, NAME, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, NAME, TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, DEFAULT_INTERFACE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, DEFAULT_INTERFACE, DESCRIPTION).set(bundle.getString("socket_binding_group.default-interface"));
        root.get(ATTRIBUTES, DEFAULT_INTERFACE, REQUIRED).set(true);
        root.get(ATTRIBUTES, DEFAULT_INTERFACE, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, DEFAULT_INTERFACE, TAIL_COMMENT_ALLOWED).set(false);
        root.get(OPERATIONS).setEmptyObject();
        root.get(CHILDREN, SOCKET_BINDING, DESCRIPTION).set(bundle.getString("socket_binding_group.socket-binding"));
        root.get(CHILDREN, SOCKET_BINDING, MIN_OCCURS).set(0);
        root.get(CHILDREN, SOCKET_BINDING, MODEL_DESCRIPTION);
        return root;
    }

    public static ModelNode getServerSocketBindingGroupAddOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = getSocketBindingGroupAdd(bundle.getString("server_socket_binding_group.add"), bundle);
        root.get(REQUEST_PROPERTIES, PORT_OFFSET, TYPE).set(ModelType.INT);
        root.get(REQUEST_PROPERTIES, PORT_OFFSET, DESCRIPTION).set(bundle.getString("server-socket_binding_group.add.port-offset"));
        root.get(REQUEST_PROPERTIES, PORT_OFFSET, REQUIRED).set(false);
        return root;
    }

    public static ModelNode getDomainSocketBindingGroupAddOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = getSocketBindingGroupAdd(bundle.getString("domain_socket_binding_group.add"), bundle);
        root.get(REQUEST_PROPERTIES, INCLUDES, TYPE).set(ModelType.LIST);
        root.get(REQUEST_PROPERTIES, INCLUDES, VALUE_TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, INCLUDES, DESCRIPTION).set(bundle.getString("domain-socket_binding_group.add.includes"));
        root.get(REQUEST_PROPERTIES, INCLUDES, REQUIRED).set(false);
        return root;
    }

    private static ModelNode getSocketBindingGroupAdd(final String description, final ResourceBundle bundle) {
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(description);
        root.get(REQUEST_PROPERTIES, DEFAULT_INTERFACE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, DEFAULT_INTERFACE, DESCRIPTION).set(bundle.getString("socket_binding_group.add.default-interface"));
        root.get(REQUEST_PROPERTIES, DEFAULT_INTERFACE, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, DEFAULT_INTERFACE, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, DEFAULT_INTERFACE, NILLABLE).set(false);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getSocketBindingGroupRemoveOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(REMOVE);
        root.get(DESCRIPTION).set(bundle.getString("socket_binding_group.remove"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }


    public static ModelNode getAddSocketBindingGroupIncludeOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(SocketBindingGroupIncludeAddHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("domain_socket_binding_group.include.add"));
        root.get(REQUEST_PROPERTIES, INCLUDE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, INCLUDE, DESCRIPTION).set(bundle.getString("domain_socket_binding_group.include.add.include"));
        root.get(REQUEST_PROPERTIES, INCLUDE, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, INCLUDE, MIN_LENGTH).set(1);
        root.get(REPLY_PROPERTIES).setEmptyObject();

        return root;
    }

    public static ModelNode getRemoveSocketBindingGroupIncludeOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(SocketBindingGroupIncludeRemoveHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("domain_socket_binding_group.include.remove"));
        root.get(REQUEST_PROPERTIES, INCLUDE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, INCLUDE, DESCRIPTION).set(bundle.getString("domain_socket_binding_group.include.remove.include"));
        root.get(REQUEST_PROPERTIES, INCLUDE, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, INCLUDE, MIN_LENGTH).set(1);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getSocketBindingDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("socket_binding"));
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("socket_binding.name"));
        root.get(ATTRIBUTES, NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, NAME, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, NAME, TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, INTERFACE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, INTERFACE, DESCRIPTION).set(bundle.getString("socket_binding.interface"));
        root.get(ATTRIBUTES, INTERFACE, REQUIRED).set(false);
        root.get(ATTRIBUTES, INTERFACE, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, INTERFACE, TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, PORT, TYPE).set(ModelType.INT);
        root.get(ATTRIBUTES, PORT, DESCRIPTION).set(bundle.getString("socket_binding.port"));
        root.get(ATTRIBUTES, PORT, REQUIRED).set(true);
        root.get(ATTRIBUTES, PORT, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, PORT, TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, FIXED_PORT, TYPE).set(ModelType.BOOLEAN);
        root.get(ATTRIBUTES, FIXED_PORT, DESCRIPTION).set(bundle.getString("socket_binding.fixed-port"));
        root.get(ATTRIBUTES, FIXED_PORT, REQUIRED).set(true);
        root.get(ATTRIBUTES, FIXED_PORT, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, FIXED_PORT, TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, MULTICAST_ADDRESS, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, MULTICAST_ADDRESS, DESCRIPTION).set(bundle.getString("socket_binding.multicast-address"));
        root.get(ATTRIBUTES, MULTICAST_ADDRESS, REQUIRED).set(false);
        root.get(ATTRIBUTES, MULTICAST_ADDRESS, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, MULTICAST_ADDRESS, TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, MULTICAST_PORT, TYPE).set(ModelType.INT);
        root.get(ATTRIBUTES, MULTICAST_PORT, DESCRIPTION).set(bundle.getString("socket_binding.multicast-port"));
        root.get(ATTRIBUTES, MULTICAST_PORT, REQUIRED).set(false);
        root.get(ATTRIBUTES, MULTICAST_PORT, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, MULTICAST_PORT, TAIL_COMMENT_ALLOWED).set(false);
        root.get(OPERATIONS).setEmptyObject();
        root.get(CHILDREN).setEmptyObject();
        return root;
    }

    public static ModelNode getSocketBindingAddOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("socket-binding.add"));
        root.get(REQUEST_PROPERTIES, INTERFACE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, INTERFACE, DESCRIPTION).set(bundle.getString("socket_binding.add.interface"));
        root.get(REQUEST_PROPERTIES, INTERFACE, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, INTERFACE, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, INTERFACE, NILLABLE).set(true);
        root.get(REQUEST_PROPERTIES, PORT, TYPE).set(ModelType.INT);
        root.get(REQUEST_PROPERTIES, PORT, DESCRIPTION).set(bundle.getString("socket_binding.add.port"));
        root.get(REQUEST_PROPERTIES, PORT, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, PORT, MIN).set(0);
        root.get(REQUEST_PROPERTIES, PORT, MAX).set(65535);
        root.get(REQUEST_PROPERTIES, FIXED_PORT, TYPE).set(ModelType.INT);
        root.get(REQUEST_PROPERTIES, FIXED_PORT, DESCRIPTION).set(bundle.getString("socket_binding.add.fixed-port"));
        root.get(REQUEST_PROPERTIES, FIXED_PORT, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, MULTICAST_ADDRESS, TYPE).set(ModelType.INT);
        root.get(REQUEST_PROPERTIES, MULTICAST_ADDRESS, DESCRIPTION).set(bundle.getString("socket_binding.add.multicast-address"));
        root.get(REQUEST_PROPERTIES, MULTICAST_ADDRESS, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, MULTICAST_ADDRESS, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, MULTICAST_ADDRESS, NILLABLE).set(true);
        root.get(REQUEST_PROPERTIES, MULTICAST_PORT, TYPE).set(ModelType.INT);
        root.get(REQUEST_PROPERTIES, MULTICAST_PORT, DESCRIPTION).set(bundle.getString("socket_binding.add.multicast-port"));
        root.get(REQUEST_PROPERTIES, MULTICAST_PORT, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, MULTICAST_PORT, MIN).set(1);
        root.get(REQUEST_PROPERTIES, MULTICAST_PORT, MAX).set(65535);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getSocketBindingRemoveOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(REMOVE);
        root.get(DESCRIPTION).set(bundle.getString("socket_binding.remove"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static void main(String[] args) {
        ModelNode binding = getSocketBindingDescription(null);
        binding.get(OPERATIONS, ADD).set(getSocketBindingAddOperation(null));
        binding.get(OPERATIONS, REMOVE).set(getSocketBindingRemoveOperation(null));
        ModelNode node = getServerSocketBindingGroupDescription(null);
        node.get(OPERATIONS, ADD).set(getServerSocketBindingGroupAddOperation(null));
        node.get(OPERATIONS, REMOVE).set(getSocketBindingGroupRemoveOperation(null));
        node.get(CHILDREN, SOCKET_BINDING, MODEL_DESCRIPTION).set(binding);
        System.out.println(node);
        node = getDomainSocketBindingGroupDescription(null);
        node.get(OPERATIONS, ADD).set(getDomainSocketBindingGroupAddOperation(null));
        node.get(OPERATIONS, REMOVE).set(getSocketBindingGroupRemoveOperation(null));
        node.get(CHILDREN, SOCKET_BINDING, MODEL_DESCRIPTION).set(binding);
        System.out.println(node);
    }
}
