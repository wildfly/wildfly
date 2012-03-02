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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROCESS_STATE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.host.controller.DirectoryGrouping;
import org.jboss.as.host.controller.operations.HostShutdownHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model description for the host model root.
 *
 * @author Brian Stansberry
 */
public class HostRootDescription {
    public static final AttributeDefinition DIRECTORY_GROUPING = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DIRECTORY_GROUPING, ModelType.STRING).
            addFlag(Flag.RESTART_ALL_SERVICES).
            setDefaultValue(DirectoryGrouping.defaultValue().toModelNode()).
            setValidator(EnumValidator.create(DirectoryGrouping.class, false, false)).
            build();

    private static final String RESOURCE_NAME = HostRootDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, HostRootDescription.class.getClassLoader(), true, true);
    }

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

        root.get(ATTRIBUTES, RELEASE_VERSION, DESCRIPTION).set(bundle.getString("host.release-version"));
        root.get(ATTRIBUTES, RELEASE_VERSION, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, RELEASE_VERSION, REQUIRED).set(true);
        root.get(ATTRIBUTES, RELEASE_VERSION, NILLABLE).set(false);
        root.get(ATTRIBUTES, RELEASE_VERSION, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, RELEASE_CODENAME, DESCRIPTION).set(bundle.getString("host.release-codename"));
        root.get(ATTRIBUTES, RELEASE_CODENAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, RELEASE_CODENAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, RELEASE_CODENAME, NILLABLE).set(false);
        root.get(ATTRIBUTES, RELEASE_CODENAME, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, PRODUCT_NAME, DESCRIPTION).set(bundle.getString("host.product-name"));
        root.get(ATTRIBUTES, PRODUCT_NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PRODUCT_NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, PRODUCT_NAME, NILLABLE).set(true);
        root.get(ATTRIBUTES, PRODUCT_NAME, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, PRODUCT_VERSION, DESCRIPTION).set(bundle.getString("host.product-version"));
        root.get(ATTRIBUTES, PRODUCT_VERSION, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PRODUCT_VERSION, REQUIRED).set(true);
        root.get(ATTRIBUTES, PRODUCT_VERSION, NILLABLE).set(true);
        root.get(ATTRIBUTES, PRODUCT_VERSION, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, MANAGEMENT_MAJOR_VERSION, DESCRIPTION).set(bundle.getString("host.management-major-version"));
        root.get(ATTRIBUTES, MANAGEMENT_MAJOR_VERSION, TYPE).set(ModelType.INT);
        root.get(ATTRIBUTES, MANAGEMENT_MAJOR_VERSION, REQUIRED).set(true);
        root.get(ATTRIBUTES, MANAGEMENT_MAJOR_VERSION, NILLABLE).set(false);
        root.get(ATTRIBUTES, MANAGEMENT_MAJOR_VERSION, MIN).set(1);

        root.get(ATTRIBUTES, MANAGEMENT_MINOR_VERSION, DESCRIPTION).set(bundle.getString("host.management-minor-version"));
        root.get(ATTRIBUTES, MANAGEMENT_MINOR_VERSION, TYPE).set(ModelType.INT);
        root.get(ATTRIBUTES, MANAGEMENT_MINOR_VERSION, REQUIRED).set(true);
        root.get(ATTRIBUTES, MANAGEMENT_MINOR_VERSION, NILLABLE).set(false);
        root.get(ATTRIBUTES, MANAGEMENT_MINOR_VERSION, MIN).set(1);

        root.get(ATTRIBUTES, PROCESS_STATE, DESCRIPTION).set(bundle.getString("host.state"));
        root.get(ATTRIBUTES, PROCESS_STATE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PROCESS_STATE, REQUIRED).set(true);
        root.get(ATTRIBUTES, PROCESS_STATE, NILLABLE).set(false);
        root.get(ATTRIBUTES, PROCESS_STATE, MIN_LENGTH).set(1);

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
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, HOST, EXPRESSIONS_ALLOWED).set(true);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, PORT, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, PORT, DESCRIPTION).set(bundle.getString("host.domain-controller.remote.port"));
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, PORT, REQUIRED).set(true);
        root.get(ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, REMOTE, VALUE_TYPE, PORT, EXPRESSIONS_ALLOWED).set(true);

        root.get(ATTRIBUTES, MASTER, DESCRIPTION).set(bundle.getString("host.master"));
        root.get(ATTRIBUTES, MASTER, TYPE).set(ModelType.BOOLEAN);

        DIRECTORY_GROUPING.addResourceAttributeDescription(bundle, "host", root);

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

        root.get(CHILDREN, CORE_SERVICE, DESCRIPTION).set(bundle.getString("host.core-services"));
        root.get(CHILDREN, CORE_SERVICE, MIN_OCCURS).set(0);
        root.get(CHILDREN, CORE_SERVICE, MODEL_DESCRIPTION);

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

    public static ModelNode getHostShutdownHandler(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(HostShutdownHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("host.shutdown"));
        root.get(REQUEST_PROPERTIES, RESTART, TYPE).set(ModelType.BOOLEAN);
        root.get(REQUEST_PROPERTIES, RESTART, DESCRIPTION).set(bundle.getString("host.shutdown.restart"));
        root.get(REQUEST_PROPERTIES, RESTART, DEFAULT).set(false);
        root.get(REQUEST_PROPERTIES, RESTART, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, RESTART, NILLABLE).set(true);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getSystemPropertiesDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return CommonDescriptions.getSystemPropertyDescription(locale, bundle.getString("host.system-property"), true);
    }

    public static ModelNode getLocalDomainControllerAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(RemoteDomainControllerAddHandler.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("host.domain-controller.local.add"));

        result.get(REQUEST_PROPERTIES).setEmptyObject();
        result.get(REPLY_PROPERTIES).setEmptyObject();
        return result;
    }

    public static ModelNode getRemoteDomainControllerAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(RemoteDomainControllerAddHandler.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("host.domain-controller.remote.add"));

        result.get(REQUEST_PROPERTIES, HOST, TYPE).set(ModelType.STRING);
        result.get(REQUEST_PROPERTIES, HOST, DESCRIPTION).set(bundle.getString("host.domain-controller.remote.host"));
        result.get(REQUEST_PROPERTIES, HOST, REQUIRED).set(true);
        result.get(REQUEST_PROPERTIES, HOST, EXPRESSIONS_ALLOWED).set(true);
        result.get(REQUEST_PROPERTIES, HOST, MIN_LENGTH).set(1);
        result.get(REQUEST_PROPERTIES, PORT, TYPE).set(ModelType.STRING);
        result.get(REQUEST_PROPERTIES, PORT, DESCRIPTION).set(bundle.getString("host.domain-controller.remote.port"));
        result.get(REQUEST_PROPERTIES, PORT, REQUIRED).set(true);
        result.get(REQUEST_PROPERTIES, PORT, EXPRESSIONS_ALLOWED).set(true);
        result.get(REQUEST_PROPERTIES, PORT, MIN).set(1);
        result.get(REQUEST_PROPERTIES, PORT, MAX).set(65535);

        result.get(REPLY_PROPERTIES).setEmptyObject();
        return result;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
