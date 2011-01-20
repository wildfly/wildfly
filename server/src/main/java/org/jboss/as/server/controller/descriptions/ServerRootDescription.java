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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROFILE_NAME;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.common.CommonAttributes;
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

        root.get(ATTRIBUTES, NAMESPACES).set(CommonAttributes.getNamespacePrefixAttribute(locale));
        root.get(ATTRIBUTES, SCHEMA_LOCATIONS).set(CommonAttributes.getSchemaLocationAttribute(locale));

        root.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("server.name"));
        root.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, NAME, REQUIRED).set(false);
        root.get(ATTRIBUTES, NAME, NILLABLE).set(true);
        root.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, MANAGEMENT, DESCRIPTION).set(bundle.getString("server.management"));
        root.get(ATTRIBUTES, MANAGEMENT, TYPE).set(ModelType.OBJECT);
        root.get(ATTRIBUTES, MANAGEMENT, VALUE_TYPE, INTERFACE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, MANAGEMENT, VALUE_TYPE, INTERFACE, DESCRIPTION).set(bundle.getString("server.management.interface"));
        root.get(ATTRIBUTES, MANAGEMENT, VALUE_TYPE, INTERFACE, REQUIRED).set(false);
        root.get(ATTRIBUTES, MANAGEMENT, VALUE_TYPE, PORT, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, MANAGEMENT, VALUE_TYPE, PORT, DESCRIPTION).set(bundle.getString("server.management.port"));
        root.get(ATTRIBUTES, MANAGEMENT, VALUE_TYPE, PORT, REQUIRED).set(false);
        root.get(ATTRIBUTES, MANAGEMENT, REQUIRED).set(true);
        root.get(ATTRIBUTES, MANAGEMENT, HEAD_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, MANAGEMENT, TAIL_COMMENT_ALLOWED).set(false);

        root.get(ATTRIBUTES, PROFILE_NAME, DESCRIPTION).set(bundle.getString("server.profile"));
        root.get(ATTRIBUTES, PROFILE_NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PROFILE_NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, PROFILE_NAME, MIN_LENGTH).set(1);
        // A bit dodgy; we'll write the comment before the <profile> tag
        root.get(ATTRIBUTES, PROFILE_NAME, HEAD_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, PROFILE_NAME, TAIL_COMMENT_ALLOWED).set(true);

        root.get(OPERATIONS);

        root.get(CHILDREN, EXTENSION, DESCRIPTION).set(bundle.getString("server.extension"));
        root.get(CHILDREN, EXTENSION, MIN_OCCURS).set(0);
        root.get(CHILDREN, EXTENSION, MODEL_DESCRIPTION);

        root.get(CHILDREN, PATH, DESCRIPTION).set(bundle.getString("server.path"));
        root.get(CHILDREN, PATH, MIN_OCCURS).set(0);
        root.get(CHILDREN, PATH, MODEL_DESCRIPTION);

        root.get(CHILDREN, SUBSYSTEM, DESCRIPTION).set(bundle.getString("server.subsystem"));
        root.get(CHILDREN, SUBSYSTEM, MIN_OCCURS).set(1);
        root.get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION);

        root.get(CHILDREN, INTERFACE, DESCRIPTION).set(bundle.getString("server.interface"));
        root.get(CHILDREN, INTERFACE, MIN_OCCURS).set(0);
        root.get(CHILDREN, INTERFACE, MODEL_DESCRIPTION);

        root.get(CHILDREN, SOCKET_BINDING_GROUP, DESCRIPTION).set(bundle.getString("server.socket-binding"));
        root.get(CHILDREN, SOCKET_BINDING_GROUP, MIN_OCCURS).set(0);
        root.get(CHILDREN, SOCKET_BINDING_GROUP, MODEL_DESCRIPTION);

        root.get(CHILDREN, SYSTEM_PROPERTY, DESCRIPTION).set(bundle.getString("server.system-property"));
        root.get(CHILDREN, SYSTEM_PROPERTY, MIN_OCCURS).set(0);
        root.get(CHILDREN, SYSTEM_PROPERTY, MODEL_DESCRIPTION);

        root.get(CHILDREN, DEPLOYMENT, DESCRIPTION).set(bundle.getString("server.deployment"));
        root.get(CHILDREN, DEPLOYMENT, MIN_OCCURS).set(0);
        root.get(CHILDREN, DEPLOYMENT, MODEL_DESCRIPTION);

        return root;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static void main(final String[] args) {
        final ModelNode root = getDescription(null);
        System.out.println(root);
    }

}
