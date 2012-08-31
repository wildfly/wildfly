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

package org.jboss.as.domain.controller.descriptions;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.server.deployment.DeploymentRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_SUBSYSTEM_ENDPOINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

/**
 * @author Emanuel Muckenhuber
 */
public class ServerGroupDescription {

    private static final String RESOURCE_NAME = ServerGroupDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getServerGroupDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("server-group"));
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(true);

        root.get(ATTRIBUTES, PROFILE, DESCRIPTION).set(bundle.getString("server-group.profile"));
        root.get(ATTRIBUTES, PROFILE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PROFILE, REQUIRED).set(true);
        root.get(ATTRIBUTES, PROFILE, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, SOCKET_BINDING_GROUP, DESCRIPTION).set(bundle.getString("server-group.socket-binding-group"));
        root.get(ATTRIBUTES, SOCKET_BINDING_GROUP, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, SOCKET_BINDING_GROUP, REQUIRED).set(true);

        root.get(ATTRIBUTES, SOCKET_BINDING_PORT_OFFSET, DESCRIPTION).set(bundle.getString("server-group.socket-binding-port-offset"));
        root.get(ATTRIBUTES, SOCKET_BINDING_PORT_OFFSET, TYPE).set(ModelType.INT);
        root.get(ATTRIBUTES, SOCKET_BINDING_PORT_OFFSET, REQUIRED).set(false);

        root.get(ATTRIBUTES, MANAGEMENT_SUBSYSTEM_ENDPOINT, DESCRIPTION).set(bundle.getString("server-group.management-subsystem-endpoint"));
        root.get(ATTRIBUTES, MANAGEMENT_SUBSYSTEM_ENDPOINT, TYPE).set(ModelType.BOOLEAN);
        root.get(ATTRIBUTES, MANAGEMENT_SUBSYSTEM_ENDPOINT, REQUIRED).set(false);
        root.get(ATTRIBUTES, MANAGEMENT_SUBSYSTEM_ENDPOINT, DEFAULT).set(false);


        root.get(OPERATIONS).setEmptyObject();

        root.get(CHILDREN, DEPLOYMENT, DESCRIPTION).set(bundle.getString("server-group.deployment"));
        root.get(CHILDREN, DEPLOYMENT, MIN_OCCURS).set(0);
        root.get(CHILDREN, DEPLOYMENT, MODEL_DESCRIPTION);

        root.get(CHILDREN, DEPLOYMENT_OVERLAY, DESCRIPTION).set(bundle.getString("server-group.deployment-overlay"));
        root.get(CHILDREN, DEPLOYMENT_OVERLAY, MIN_OCCURS).set(0);
        root.get(CHILDREN, DEPLOYMENT_OVERLAY, MODEL_DESCRIPTION);

        root.get(CHILDREN, JVM, DESCRIPTION).set(bundle.getString("server-group.jvm"));
        root.get(CHILDREN, JVM, MIN_OCCURS).set(0);
        root.get(CHILDREN, JVM, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, JVM, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, SYSTEM_PROPERTY, DESCRIPTION).set(bundle.getString("server-group.system-property"));
        root.get(CHILDREN, SYSTEM_PROPERTY, MIN_OCCURS).set(0);
        root.get(CHILDREN, SYSTEM_PROPERTY, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, SYSTEM_PROPERTY, MODEL_DESCRIPTION).setEmptyObject();

        return root;
    }

    public static ModelNode getServerGroupAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();

        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("server-group.add"));

        root.get(REQUEST_PROPERTIES, PROFILE, DESCRIPTION).set(bundle.getString("server-group.profile"));
        root.get(REQUEST_PROPERTIES, PROFILE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, PROFILE, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, PROFILE, MIN_LENGTH).set(1);

        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_GROUP, DESCRIPTION).set(bundle.getString("server-group.socket-binding-group"));
        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_GROUP, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_GROUP, REQUIRED).set(true);

        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_PORT_OFFSET, DESCRIPTION).set(bundle.getString("server-group.socket-binding-port-offset"));
        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_PORT_OFFSET, TYPE).set(ModelType.INT);
        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_PORT_OFFSET, REQUIRED).set(false);

        root.get(REQUEST_PROPERTIES, JVM, DESCRIPTION).set(bundle.getString("server-group.add.jvm"));
        root.get(REQUEST_PROPERTIES, JVM, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, JVM, REQUIRED).set(false);

        root.get(REQUEST_PROPERTIES, MANAGEMENT_SUBSYSTEM_ENDPOINT, DESCRIPTION).set(bundle.getString("server-group.management-subsystem-endpoint"));
        root.get(REQUEST_PROPERTIES, MANAGEMENT_SUBSYSTEM_ENDPOINT, TYPE).set(ModelType.BOOLEAN);
        root.get(REQUEST_PROPERTIES, MANAGEMENT_SUBSYSTEM_ENDPOINT, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, MANAGEMENT_SUBSYSTEM_ENDPOINT, DEFAULT).set(false);

        return root;
    }

    public static ModelNode getServerGroupRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();

        root.get(OPERATION_NAME).set(REMOVE);
        root.get(DESCRIPTION).set(bundle.getString("server-group.remove"));

        return root;
    }

    public static ModelNode getDeploymentRemoveOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DeploymentRemoveHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("server-group.deployment.remove"));
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getRestartServersOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DomainServerLifecycleHandlers.RESTART_SERVERS_NAME);
        root.get(DESCRIPTION).set(bundle.getString("server-group.servers.restart"));
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getStopServersOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DomainServerLifecycleHandlers.STOP_SERVERS_NAME);
        root.get(DESCRIPTION).set(bundle.getString("server-group.servers.stop"));
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getStartServersOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DomainServerLifecycleHandlers.START_SERVERS_NAME);
        root.get(DESCRIPTION).set(bundle.getString("server-group.servers.start"));
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getSystemPropertiesDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return CommonDescriptions.getSystemPropertyDescription(locale, bundle.getString("server-group.system-property"), true);
    }


    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
