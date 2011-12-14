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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model description for profile elements.
 *
 * @author Brian Stansberry
 */
public class ProfileDescription {

    private static final String RESOURCE_NAME = ProfileDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getProfileDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = getBasicProfileDescription(bundle);
        appendSubsystemChild(root, bundle);
        return root;
    }

    private static ModelNode getBasicProfileDescription(final ResourceBundle bundle) {
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("profile"));
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, NAME, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, NAME, TAIL_COMMENT_ALLOWED).set(false);
        root.get(OPERATIONS).setEmptyObject();
        return root;
    }

    public static ModelNode getProfileWithIncludesDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = getBasicProfileDescription(bundle);
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        /* This will be reintroduced for 7.2.0, leave commented out
        root.get(ATTRIBUTES, INCLUDES, DESCRIPTION).set(bundle.getString("profile.includes"));
        root.get(ATTRIBUTES, INCLUDES, TYPE).set(ModelType.LIST);
        root.get(ATTRIBUTES, INCLUDES, VALUE_TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, INCLUDES, REQUIRED).set(false);
        */
        appendSubsystemChild(root, bundle);
        return root;
    }

    public static ModelNode getProfileIncludesDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("profile.include"));
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, PROFILE, TYPE).set(ModelType.LIST);
        root.get(ATTRIBUTES, PROFILE, DESCRIPTION).set(bundle.getString("profile.include.profile"));
        root.get(ATTRIBUTES, PROFILE, REQUIRED).set(true);
        root.get(ATTRIBUTES, PROFILE, VALUE_TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PROFILE, HEAD_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, PROFILE, TAIL_COMMENT_ALLOWED).set(false);
        root.get(OPERATIONS).setEmptyObject();
        return root;
    }

    private static void appendSubsystemChild(final ModelNode root, final ResourceBundle bundle) {
        root.get(CHILDREN, SUBSYSTEM, DESCRIPTION).set(bundle.getString("profile.subsystem"));
        root.get(CHILDREN, SUBSYSTEM, MIN_OCCURS).set(1);
        root.get(CHILDREN, SUBSYSTEM, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION).setEmptyObject();
    }

    public static ModelNode getProfileAddOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("profile.add"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        /* This will be reintroduced for 7.2.0, leave commented out
        root.get(REQUEST_PROPERTIES, INCLUDES, TYPE).set(ModelType.LIST);
        root.get(REQUEST_PROPERTIES, INCLUDES, DESCRIPTION).set(bundle.getString("profile.add.includes"));
        root.get(REQUEST_PROPERTIES, INCLUDES, VALUE_TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, INCLUDES, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, INCLUDES, NILLABLE).set(true);
        */

        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getProfileDescribeOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("profile.describe"));
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
        root.get(REPLY_PROPERTIES, VALUE_TYPE).set(ModelType.OBJECT);
        return root;
    }

    public static ModelNode getProfileRemoveOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(REMOVE);
        root.get(DESCRIPTION).set(bundle.getString("profile.remove"));
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
}
