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
 */package org.jboss.as.server.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DUMP_SERVICES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SHUTDOWN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.LAUNCH_TYPE;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROCESS_TYPE;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROFILE_NAME;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROCESS_STATE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model description for the server root.
 *
 * @author Brian Stansberry
 */
public class ServerRootDescription {

    private static final String RESOURCE_NAME = ServerRootDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getDescription(final Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("server"));
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(true);

        root.get(ATTRIBUTES, NAMESPACES).set(CommonDescriptions.getNamespacePrefixAttribute(locale));
        root.get(ATTRIBUTES, SCHEMA_LOCATIONS).set(CommonDescriptions.getSchemaLocationAttribute(locale));

        root.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("server.name"));
        root.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, NAME, REQUIRED).set(false);
        root.get(ATTRIBUTES, NAME, NILLABLE).set(true);
        root.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, RELEASE_VERSION, DESCRIPTION).set(bundle.getString("server.release-version"));
        root.get(ATTRIBUTES, RELEASE_VERSION, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, RELEASE_VERSION, REQUIRED).set(true);
        root.get(ATTRIBUTES, RELEASE_VERSION, NILLABLE).set(false);
        root.get(ATTRIBUTES, RELEASE_VERSION, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, RELEASE_CODENAME, DESCRIPTION).set(bundle.getString("server.release-codename"));
        root.get(ATTRIBUTES, RELEASE_CODENAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, RELEASE_CODENAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, RELEASE_CODENAME, NILLABLE).set(false);
        root.get(ATTRIBUTES, RELEASE_CODENAME, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, PRODUCT_NAME, DESCRIPTION).set(bundle.getString("server.product-name"));
        root.get(ATTRIBUTES, PRODUCT_NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PRODUCT_NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, PRODUCT_NAME, NILLABLE).set(true);
        root.get(ATTRIBUTES, PRODUCT_NAME, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, PRODUCT_VERSION, DESCRIPTION).set(bundle.getString("server.product-version"));
        root.get(ATTRIBUTES, PRODUCT_VERSION, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PRODUCT_VERSION, REQUIRED).set(true);
        root.get(ATTRIBUTES, PRODUCT_VERSION, NILLABLE).set(true);
        root.get(ATTRIBUTES, PRODUCT_VERSION, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, MANAGEMENT_MAJOR_VERSION, DESCRIPTION).set(bundle.getString("server.management-major-version"));
        root.get(ATTRIBUTES, MANAGEMENT_MAJOR_VERSION, TYPE).set(ModelType.INT);
        root.get(ATTRIBUTES, MANAGEMENT_MAJOR_VERSION, REQUIRED).set(true);
        root.get(ATTRIBUTES, MANAGEMENT_MAJOR_VERSION, NILLABLE).set(false);
        root.get(ATTRIBUTES, MANAGEMENT_MAJOR_VERSION, MIN).set(1);

        root.get(ATTRIBUTES, MANAGEMENT_MINOR_VERSION, DESCRIPTION).set(bundle.getString("server.management-minor-version"));
        root.get(ATTRIBUTES, MANAGEMENT_MINOR_VERSION, TYPE).set(ModelType.INT);
        root.get(ATTRIBUTES, MANAGEMENT_MINOR_VERSION, REQUIRED).set(true);
        root.get(ATTRIBUTES, MANAGEMENT_MINOR_VERSION, NILLABLE).set(false);
        root.get(ATTRIBUTES, MANAGEMENT_MINOR_VERSION, MIN).set(1);

        root.get(ATTRIBUTES, PROFILE_NAME, DESCRIPTION).set(bundle.getString("server.profile"));
        root.get(ATTRIBUTES, PROFILE_NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PROFILE_NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, PROFILE_NAME, MIN_LENGTH).set(1);
        // A bit dodgy; we'll write the comment before the <profile> tag
        root.get(ATTRIBUTES, PROFILE_NAME, HEAD_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, PROFILE_NAME, TAIL_COMMENT_ALLOWED).set(true);

        root.get(ATTRIBUTES, PROCESS_STATE, DESCRIPTION).set(bundle.getString("server.state"));
        root.get(ATTRIBUTES, PROCESS_STATE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PROCESS_STATE, REQUIRED).set(true);
        root.get(ATTRIBUTES, PROCESS_STATE, NILLABLE).set(false);
        root.get(ATTRIBUTES, PROCESS_STATE, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, PROCESS_TYPE, DESCRIPTION).set(bundle.getString("server.process-type"));
        root.get(ATTRIBUTES, PROCESS_TYPE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PROCESS_TYPE, REQUIRED).set(true);
        root.get(ATTRIBUTES, PROCESS_TYPE, NILLABLE).set(false);
        root.get(ATTRIBUTES, PROCESS_TYPE, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, LAUNCH_TYPE, DESCRIPTION).set(bundle.getString("server.launch-type"));
        root.get(ATTRIBUTES, LAUNCH_TYPE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, LAUNCH_TYPE, REQUIRED).set(true);
        root.get(ATTRIBUTES, LAUNCH_TYPE, NILLABLE).set(false);
        root.get(ATTRIBUTES, LAUNCH_TYPE, MIN_LENGTH).set(1);
        root.get(ATTRIBUTES, LAUNCH_TYPE, ALLOWED).add(ServerEnvironment.LaunchType.DOMAIN.toString());
        root.get(ATTRIBUTES, LAUNCH_TYPE, ALLOWED).add(ServerEnvironment.LaunchType.STANDALONE.toString());
        root.get(ATTRIBUTES, LAUNCH_TYPE, ALLOWED).add(ServerEnvironment.LaunchType.EMBEDDED.toString());

        root.get(OPERATIONS);

        root.get(CHILDREN, EXTENSION, DESCRIPTION).set(bundle.getString("server.extension"));
        root.get(CHILDREN, EXTENSION, MIN_OCCURS).set(0);
        root.get(CHILDREN, EXTENSION, MODEL_DESCRIPTION);

        root.get(CHILDREN, PATH, DESCRIPTION).set(bundle.getString("server.path"));
        root.get(CHILDREN, PATH, MIN_OCCURS).set(0);
        root.get(CHILDREN, PATH, MODEL_DESCRIPTION);

        root.get(CHILDREN, SYSTEM_PROPERTY, DESCRIPTION).set(bundle.getString("server.system-properties"));
        root.get(CHILDREN, SYSTEM_PROPERTY, MIN_OCCURS).set(0);
        root.get(CHILDREN, SYSTEM_PROPERTY, MODEL_DESCRIPTION);

        root.get(CHILDREN, CORE_SERVICE, DESCRIPTION).set(bundle.getString("server.core-services"));
        root.get(CHILDREN, CORE_SERVICE, MIN_OCCURS).set(0);
        root.get(CHILDREN, CORE_SERVICE, MODEL_DESCRIPTION);

        root.get(CHILDREN, INTERFACE, DESCRIPTION).set(bundle.getString("server.interface"));
        root.get(CHILDREN, INTERFACE, MIN_OCCURS).set(0);
        root.get(CHILDREN, INTERFACE, MODEL_DESCRIPTION);

        root.get(CHILDREN, SOCKET_BINDING_GROUP, DESCRIPTION).set(bundle.getString("server.socket-binding"));
        root.get(CHILDREN, SOCKET_BINDING_GROUP, MIN_OCCURS).set(0);
        root.get(CHILDREN, SOCKET_BINDING_GROUP, MODEL_DESCRIPTION);

        root.get(CHILDREN, DEPLOYMENT, DESCRIPTION).set(bundle.getString("server.deployment"));
        root.get(CHILDREN, DEPLOYMENT, MIN_OCCURS).set(0);
        root.get(CHILDREN, DEPLOYMENT, MODEL_DESCRIPTION);

        root.get(CHILDREN, SUBSYSTEM, DESCRIPTION).set(bundle.getString("server.subsystem"));
        root.get(CHILDREN, SUBSYSTEM, MIN_OCCURS).set(0);
        root.get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION);


        return root;
    }

    public static ModelNode getCompositeOperationDescription(Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(COMPOSITE);
        root.get(DESCRIPTION).set(bundle.getString("composite"));
        root.get(REQUEST_PROPERTIES, STEPS, TYPE).set(ModelType.LIST); // TODO details of the type
        root.get(REQUEST_PROPERTIES, STEPS, DESCRIPTION).set(bundle.getString("composite.steps"));
        root.get(REQUEST_PROPERTIES, STEPS, REQUIRED).set(true);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
        // TODO details of the reply
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("composite.result"));
        return root;
    }

    /** {@inheritDoc} */
    public static ModelNode getShutdownOperationDescription(final Locale locale) {
        ResourceBundle bundle = getResourceBundle(locale);

        ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(SHUTDOWN);
        node.get(DESCRIPTION).set(bundle.getString("shutdown"));
        node.get(REQUEST_PROPERTIES, RESTART, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, RESTART, DESCRIPTION).set(bundle.getString("shutdown.restart"));
        node.get(REQUEST_PROPERTIES, RESTART, DEFAULT).set(false);
        node.get(REQUEST_PROPERTIES, RESTART, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, RESTART, NILLABLE).set(true);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    public static ModelNode getDumpServicesOperationDescription(final Locale locale) {
        ResourceBundle bundle = getResourceBundle(locale);

        ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(DUMP_SERVICES);
        node.get(DESCRIPTION).set(bundle.getString("dump-services"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        return node;
    }

     public static ModelNode getRestartRequiredDescription(final Locale locale) {
        ResourceBundle bundle = getResourceBundle(locale);

        ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ServerRestartRequiredHandler.OPERATION_NAME);
        node.get(DESCRIPTION).set(bundle.getString("restart-required"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
