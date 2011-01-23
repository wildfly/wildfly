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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CRITERIA;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model descriptions for interface elements.
 *
 * @author Brian Stansberry
 */
public class InterfaceDescription {

    private static final String RESOURCE_NAME = InterfaceDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getNamedInterfaceDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("named_interface"));
        populateInterface(root, bundle, false);
        return root;
    }

    public static ModelNode getSpecifiedInterfaceDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("specified_interface"));
        populateInterface(root, bundle, true);
        return root;
    }

    private static void populateInterface(ModelNode root, ResourceBundle bundle, boolean specified) {
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("interface.name"));
        root.get(ATTRIBUTES, NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, NAME, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, NAME, TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, CRITERIA, TYPE).set(ModelType.OBJECT);
        // FIXME need a value-type or something similar
        root.get(ATTRIBUTES, CRITERIA, DESCRIPTION).set(bundle.getString("interface.criteria"));
        root.get(ATTRIBUTES, CRITERIA, REQUIRED).set(specified);
        root.get(ATTRIBUTES, CRITERIA, HEAD_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, CRITERIA, TAIL_COMMENT_ALLOWED).set(false);
        root.get(OPERATIONS).setEmptyObject();
    }

    public static ModelNode getNamedInterfaceAddOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("interface.add"));
        root.get(REQUEST_PROPERTIES, CRITERIA, TYPE).set(ModelType.OBJECT);
        root.get(REQUEST_PROPERTIES, CRITERIA, DESCRIPTION).set(bundle.getString("interface.add.criteria"));
        root.get(REQUEST_PROPERTIES, CRITERIA, REQUIRED).set(false);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getSpecifiedInterfaceAddOperation(final Locale locale) {
        final ModelNode root = getNamedInterfaceAddOperation(locale);
        root.get(REQUEST_PROPERTIES, CRITERIA, REQUIRED).set(true);
        return root;
    }

    public static ModelNode getInterfaceRemoveOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(REMOVE);
        root.get(DESCRIPTION).set(bundle.getString("interface.remove"));
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
        ModelNode node = getNamedInterfaceDescription(null);
        node.get(OPERATIONS, ADD).set(getNamedInterfaceAddOperation(null));
        node.get(OPERATIONS, REMOVE).set(getInterfaceRemoveOperation(null));
        System.out.println(node);
        node = getSpecifiedInterfaceDescription(null);
        node.get(OPERATIONS, ADD).set(getSpecifiedInterfaceAddOperation(null));
        node.get(OPERATIONS, REMOVE).set(getInterfaceRemoveOperation(null));
        System.out.println(node);
    }
}
