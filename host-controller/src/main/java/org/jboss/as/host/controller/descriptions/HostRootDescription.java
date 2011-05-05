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
 */package org.jboss.as.host.controller.descriptions;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Model description for the host model root.
 *
 * @author Brian Stansberry
 */
public class HostRootDescription {

    private static final String RESOURCE_NAME = HostRootDescription.class.getPackage().getName() + ".LocalDescriptions";

    private static final String DOMAIN_CONTROLLER = "domain-controller";
    private static final String SERVER_CONFIG = "server-config";

    public static ModelNode getDescription(final Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("host"));
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, NAMESPACES).set(CommonDescriptions.getNamespacePrefixAttribute(locale));
        root.get(ATTRIBUTES, SCHEMA_LOCATIONS).set(CommonDescriptions.getSchemaLocationAttribute(locale));

        root.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("host.name"));
        root.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, NAME, REQUIRED).set(false);
        root.get(ATTRIBUTES, NAME, NILLABLE).set(true);
        root.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, DESCRIPTION).set(bundle.getString("host.domain-controller"));
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, TYPE).set(ModelType.OBJECT);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, REQUIRED).set(true);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, HEAD_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, TAIL_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, LOCAL, TYPE).set(ModelType.OBJECT);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, LOCAL, DESCRIPTION).set(bundle.getString("host.domain-controller.local"));
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, LOCAL, REQUIRED).set(false);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, TYPE).set(ModelType.OBJECT);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, DESCRIPTION).set(bundle.getString("host.domain-controller.remote"));
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, HOST, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, HOST, DESCRIPTION).set(bundle.getString("host.domain-controller.remote.host"));
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, HOST, REQUIRED).set(true);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, PORT, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, PORT, DESCRIPTION).set(bundle.getString("host.domain-controller.remote.port"));
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, PORT, REQUIRED).set(true);

        root.get(OPERATIONS).setEmptyObject();

        root.get(CHILDREN, EXTENSION, DESCRIPTION).set(bundle.getString("host.extension"));
        root.get(CHILDREN, EXTENSION, MIN_OCCURS).set(0);
        root.get(CHILDREN, EXTENSION, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, EXTENSION, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, PATH, DESCRIPTION).set(bundle.getString("host.path"));
        root.get(CHILDREN, PATH, MIN_OCCURS).set(0);
        root.get(CHILDREN, PATH, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, PATH, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, SYSTEM_PROPERTY, DESCRIPTION).set(bundle.getString("host.system-properties"));
        root.get(CHILDREN, SYSTEM_PROPERTY, MIN_OCCURS).set(0);
        root.get(CHILDREN, SYSTEM_PROPERTY, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, SYSTEM_PROPERTY, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, NATIVE_INTERFACE).set(ManagementDescription.getNativeManagementDescription(locale));
        root.get(CHILDREN, HTTP_INTERFACE).set(ManagementDescription.getHttpManagementDescription(locale));

        root.get(CHILDREN, INTERFACE, DESCRIPTION).set(bundle.getString("host.interface"));
        root.get(CHILDREN, INTERFACE, MIN_OCCURS).set(0);
        root.get(CHILDREN, INTERFACE, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, INTERFACE, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, JVM, DESCRIPTION).set(bundle.getString("host.jvm"));
        root.get(CHILDREN, JVM, MIN_OCCURS).set(0);
        root.get(CHILDREN, JVM, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, JVM, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, SERVER_CONFIG, DESCRIPTION).set(bundle.getString("host.server-config"));
        root.get(CHILDREN, SERVER_CONFIG, MIN_OCCURS).set(0);
        root.get(CHILDREN, SERVER_CONFIG, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, SERVER_CONFIG, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, RUNNING_SERVER, DESCRIPTION).set(bundle.getString("host.server"));
        root.get(CHILDREN, RUNNING_SERVER, MIN_OCCURS).set(0);
        root.get(CHILDREN, RUNNING_SERVER, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, RUNNING_SERVER, MODEL_DESCRIPTION).setEmptyObject();
        return root;
    }

    public static ModelNode getStartServerOperation(final Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set("start-server");
        root.get(DESCRIPTION).set(bundle.getString("host.start-server"));
        root.get(REQUEST_PROPERTIES, SERVER, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SERVER, DESCRIPTION).set(bundle.getString("host.start-server.server"));
        root.get(REQUEST_PROPERTIES, SERVER, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, SERVER, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, SERVER, NILLABLE).set(false);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("host.start-server.reply"));
        return root;
    }

    public static ModelNode getRestartServerOperation(final Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set("restart-server");
        root.get(DESCRIPTION).set(bundle.getString("host.restart-server"));
        root.get(REQUEST_PROPERTIES, SERVER, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SERVER, DESCRIPTION).set(bundle.getString("host.restart-server.server"));
        root.get(REQUEST_PROPERTIES, SERVER, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, SERVER, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, SERVER, NILLABLE).set(false);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("host.restart-server.reply"));
        return root;
    }

    public static ModelNode getStopServerOperation(final Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set("stop-server");
        root.get(DESCRIPTION).set(bundle.getString("host.stop-server"));
        root.get(REQUEST_PROPERTIES, SERVER, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SERVER, DESCRIPTION).set(bundle.getString("host.stop-server.server"));
        root.get(REQUEST_PROPERTIES, SERVER, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, SERVER, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, SERVER, NILLABLE).set(false);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("host.stop-server.reply"));
        return root;
    }

    public static ModelNode getSystemPropertiesDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return CommonDescriptions.getSystemPropertyDescription(locale, bundle.getString("host.system-property"), false);
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
