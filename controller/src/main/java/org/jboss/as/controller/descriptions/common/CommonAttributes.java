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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.operations.common.AddNamespaceHandler;
import org.jboss.as.controller.operations.common.AddSchemaLocationHandler;
import org.jboss.as.controller.operations.common.RemoveNamespaceHandler;
import org.jboss.as.controller.operations.common.RemoveSchemaLocationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Some bits and pieces of model attribute description that are used across models.
 *
 * @author Brian Stansberry
 */
public class CommonAttributes {

    private static final String RESOURCE_NAME = CommonAttributes.class.getPackage().getName() + ".LocalDescriptions";

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
        root.get(OPERATION_NAME).set(AddNamespaceHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("namespaces.add"));
        root.get(REQUEST_PROPERTIES, NAMESPACE, TYPE).set(ModelType.PROPERTY);
        root.get(REQUEST_PROPERTIES, NAMESPACE, VALUE_TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, NAMESPACE, DESCRIPTION).set(bundle.getString("namespaces.add.namespace"));
        root.get(REQUEST_PROPERTIES, NAMESPACE, REQUIRED).set(true);
        root.get(REPLY_PROPERTIES).setEmptyObject();

        return root;
    }

    public static ModelNode getRemoveNamespaceOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(RemoveNamespaceHandler.OPERATION_NAME);
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
        root.get(OPERATION_NAME).set(AddSchemaLocationHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("schema-locations.add"));
        root.get(REQUEST_PROPERTIES, SCHEMA_LOCATION, TYPE).set(ModelType.PROPERTY);
        root.get(REQUEST_PROPERTIES, SCHEMA_LOCATION, VALUE_TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SCHEMA_LOCATION, DESCRIPTION).set(bundle.getString("schema-locations.add.schema-location"));
        root.get(REQUEST_PROPERTIES, SCHEMA_LOCATION, REQUIRED).set(true);
        root.get(REPLY_PROPERTIES).setEmptyObject();

        return root;

    }

    public static ModelNode getRemoveSchemaLocationOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(RemoveSchemaLocationHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("schema-locations.remove"));
        root.get(REQUEST_PROPERTIES, SCHEMA_LOCATION, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SCHEMA_LOCATION, DESCRIPTION).set(bundle.getString("schema-locations.remove.schema-location"));
        root.get(REQUEST_PROPERTIES, SCHEMA_LOCATION, REQUIRED).set(true);
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
        System.out.println(getSchemaLocationAttribute(null));
        System.out.println(getNamespacePrefixAttribute(null));
        System.out.println(getAddNamespaceOperation(null));
        System.out.println(getRemoveNamespaceOperation(null));
        System.out.println(getAddSchemaLocationOperation(null));
        System.out.println(getRemoveSchemaLocationOperation(null));

    }
}
