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
 */package org.jboss.as.controller.descriptions.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROBLEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URI;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyRemoveHandler;
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.controller.operations.common.ValidateOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Some bits and pieces of model attribute description that are used across models.
 *
 * @author Brian Stansberry
 */
public class CommonDescriptions {

    private static final String RESOURCE_NAME = CommonDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return getResourceDescriptionResolver(keyPrefix, true);
    }

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix, boolean useUnprefixedChildTypes) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, CommonDescriptions.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    public static ModelNode getNamespacePrefixAttribute(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(TYPE).set(ModelType.OBJECT);
        root.get(VALUE_TYPE).set(ModelType.STRING);
        root.get(DESCRIPTION).set(bundle.getString("namespaces"));
        root.get(REQUIRED).set(false);
        root.get(HEAD_COMMENT_ALLOWED).set(false);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        return root;
    }

    public static ModelNode getSchemaLocationAttribute(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(TYPE).set(ModelType.OBJECT);
        root.get(VALUE_TYPE).set(ModelType.STRING);
        root.get(DESCRIPTION).set(bundle.getString("schema-locations"));
        root.get(REQUIRED).set(false);
        root.get(HEAD_COMMENT_ALLOWED).set(false);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        return root;
    }

    public static ModelNode getAddNamespaceOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(NamespaceAddHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("namespaces.add"));
        root.get(REQUEST_PROPERTIES, NAMESPACE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, NAMESPACE, DESCRIPTION).set(bundle.getString("namespaces.add.prefix"));
        root.get(REQUEST_PROPERTIES, NAMESPACE, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, URI, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, URI, DESCRIPTION).set(bundle.getString("namespaces.add.uri"));
        root.get(REQUEST_PROPERTIES, URI, REQUIRED).set(true);
        root.get(REPLY_PROPERTIES).setEmptyObject();

        return root;
    }

    public static ModelNode getRemoveNamespaceOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(NamespaceRemoveHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("namespaces.remove"));
        root.get(REQUEST_PROPERTIES, NAMESPACE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, NAMESPACE, DESCRIPTION).set(bundle.getString("namespaces.remove.namespace"));
        root.get(REQUEST_PROPERTIES, NAMESPACE, REQUIRED).set(true);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getAddSchemaLocationOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(SchemaLocationAddHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("schema-locations.add"));
        root.get(REQUEST_PROPERTIES, URI, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, URI, DESCRIPTION).set(bundle.getString("schema-locations.add.uri"));
        root.get(REQUEST_PROPERTIES, URI, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, SCHEMA_LOCATION, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SCHEMA_LOCATION, DESCRIPTION).set(bundle.getString("schema-locations.add.schema-location"));
        root.get(REQUEST_PROPERTIES, SCHEMA_LOCATION, REQUIRED).set(true);
        root.get(REPLY_PROPERTIES).setEmptyObject();

        return root;

    }

    public static ModelNode getRemoveSchemaLocationOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(SchemaLocationRemoveHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("schema-locations.remove"));
        root.get(REQUEST_PROPERTIES, URI, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, URI, DESCRIPTION).set(bundle.getString("schema-locations.remove.schema-uri"));
        root.get(REQUEST_PROPERTIES, URI, REQUIRED).set(true);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getSystemPropertyDescription(final Locale locale, final String description, final boolean useBoottime) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(description);
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, VALUE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, VALUE, DESCRIPTION).set(bundle.getString("system-property.value"));
        root.get(ATTRIBUTES, VALUE, REQUIRED).set(false);
        if (useBoottime) {
            root.get(ATTRIBUTES, BOOT_TIME, TYPE).set(ModelType.BOOLEAN);
            root.get(ATTRIBUTES, BOOT_TIME, DESCRIPTION).set(bundle.getString("system-property.boottime"));
            root.get(ATTRIBUTES, BOOT_TIME, REQUIRED).set(false);
            root.get(ATTRIBUTES, BOOT_TIME, DEFAULT).set(true);
        }
        return root;
    }

    public static ModelNode getAddSystemPropertyOperation(final Locale locale, final boolean useBoottime) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(SystemPropertyAddHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("system-property.add"));
        root.get(REQUEST_PROPERTIES, VALUE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, VALUE, DESCRIPTION).set(bundle.getString("system-property.value"));
        root.get(REQUEST_PROPERTIES, VALUE, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, VALUE, NILLABLE).set(true);
        if (useBoottime) {
            root.get(REQUEST_PROPERTIES, BOOT_TIME, TYPE).set(ModelType.STRING);
            root.get(REQUEST_PROPERTIES, BOOT_TIME, DESCRIPTION).set(bundle.getString("system-property.boottime"));
            root.get(REQUEST_PROPERTIES, BOOT_TIME, REQUIRED).set(false);
            root.get(REQUEST_PROPERTIES, BOOT_TIME, NILLABLE).set(true);
            root.get(REQUEST_PROPERTIES, BOOT_TIME, DEFAULT).set(true);
        }
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getRemoveSystemPropertyOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(SystemPropertyRemoveHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("system-property.remove"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final String READ_CONFIG_AS_XML = "read-config-as-xml";
    public static ModelNode getReadConfigAsXmlOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(READ_CONFIG_AS_XML);
        root.get(DESCRIPTION).set(bundle.getString(READ_CONFIG_AS_XML));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString(READ_CONFIG_AS_XML + ".response"));
        return root;
    }

    public static ModelNode getValidateAddressOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode descr = getSingleParamSimpleReplyOperation(bundle, ValidateAddressOperationHandler.OPERATION_NAME, "global", VALUE, ModelType.OBJECT, false, ModelType.OBJECT, true);
        final ModelNode valueType = descr.get(REPLY_PROPERTIES, VALUE_TYPE);
        valueType.get(VALID, DESCRIPTION).set(bundle.getString("global." + ValidateAddressOperationHandler.OPERATION_NAME + ".reply." + VALID));
        valueType.get(VALID, TYPE).set(ModelType.BOOLEAN);
        valueType.get(PROBLEM, DESCRIPTION).set(bundle.getString("global." + ValidateAddressOperationHandler.OPERATION_NAME + ".reply." + PROBLEM));
        valueType.get(PROBLEM, TYPE).set(ModelType.STRING);
        return descr;
    }

    public static ModelNode getCompositeOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode descr = getSingleParamSimpleReplyOperation(bundle, COMPOSITE, "root", STEPS, ModelType.LIST, false, ModelType.OBJECT, true);
        final ModelNode steps = descr.get(REQUEST_PROPERTIES, STEPS);
        steps.get(VALUE_TYPE).set(ModelType.OBJECT);
        // TODO could probably describe better the response structure
        // and make it different for domain and standalone mode
        return descr;
    }

    public static ModelNode getSubsystemDescribeOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("subsystem.describe"));
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
        root.get(REPLY_PROPERTIES, VALUE_TYPE).set(ModelType.OBJECT);
        return root;
    }

    public static ModelNode getServiceContainerDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("core.service-container"));
        root.get(HEAD_COMMENT_ALLOWED).set(false);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        root.get(OPERATIONS);
        return root;
    }

    public static ModelNode getValidateOperationDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return getSingleParamOnlyOperation(bundle, ValidateOperationHandler.OPERATION_NAME, "global", VALUE, ModelType.OBJECT, false);
    }

    public static ModelNode getDescriptionOnlyOperation(final ResourceBundle bundle, final String operationName, final String descriptionPrefix) {

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(operationName);
        String descriptionKey = descriptionPrefix == null ? operationName : descriptionPrefix + "." + operationName;
        node.get(DESCRIPTION).set(bundle.getString(descriptionKey));

        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    public static ModelNode getSingleParamOnlyOperation(final ResourceBundle bundle, final String operationName,
                                                         final String descriptionPrefix, final String paramName,
                                                        final ModelType paramType, final boolean nillable) {

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(operationName);
        String descriptionKey = descriptionPrefix == null ? operationName : descriptionPrefix + "." + operationName;
        node.get(DESCRIPTION).set(bundle.getString(descriptionKey));

        final ModelNode param = node.get(REQUEST_PROPERTIES, paramName);
        param.get(DESCRIPTION).set(bundle.getString(descriptionKey + "." + paramName));
        param.get(TYPE).set(paramType);
        param.get(REQUIRED).set(!nillable);
        param.get(NILLABLE).set(nillable);

        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    public static ModelNode getNoArgSimpleReplyOperation(final ResourceBundle bundle, final String operationName,
                                                         final String descriptionPrefix, final ModelType replyType,
                                                         final boolean describeReply) {
        final ModelNode result = getDescriptionOnlyOperation(bundle, operationName, descriptionPrefix);
        if (describeReply) {
            String replyKey = descriptionPrefix == null ? operationName + ".reply" : descriptionPrefix + "." + operationName + ".reply";
            result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString(replyKey));
        }
        result.get(REPLY_PROPERTIES, TYPE).set(replyType);

        return result;
    }

    public static ModelNode getSingleParamSimpleReplyOperation(final ResourceBundle bundle, final String operationName,
                                                         final String descriptionPrefix, final String paramName,
                                                         final ModelType paramType, final boolean paramNillable,
                                                         final ModelType replyType, final boolean describeReply) {
        final ModelNode result = getSingleParamOnlyOperation(bundle, operationName, descriptionPrefix, paramName, paramType, paramNillable);
        if (describeReply) {
            String replyKey = descriptionPrefix == null ? operationName + ".reply" : descriptionPrefix + "." + operationName + ".reply";
            result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString(replyKey));
        }
        result.get(REPLY_PROPERTIES, TYPE).set(replyType);

        return result;
    }

    public static ModelNode getNoArgSimpleListReplyOperation(final ResourceBundle bundle, final String operationName,
                                                         final String descriptionPrefix, final ModelType listValueType,
                                                         final boolean describeReply) {
        ModelNode result = getNoArgSimpleReplyOperation(bundle, operationName, descriptionPrefix, ModelType.LIST, describeReply);
        result.get(REPLY_PROPERTIES, VALUE_TYPE).set(listValueType);
        return result;
    }

    public static ModelNode getSingleParamSimpleListReplyOperation(final ResourceBundle bundle, final String operationName,
                                                         final String descriptionPrefix, final String paramName,
                                                         final ModelType paramType, final boolean paramNillable,
                                                         final ModelType listValueType, final boolean describeReply) {
        ModelNode result = getSingleParamSimpleReplyOperation(bundle, operationName, descriptionPrefix, paramName,
                paramType, paramNillable, ModelType.LIST, describeReply);
        result.get(REPLY_PROPERTIES, VALUE_TYPE).set(listValueType);
        return result;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
