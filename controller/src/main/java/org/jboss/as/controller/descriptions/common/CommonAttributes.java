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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

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

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
