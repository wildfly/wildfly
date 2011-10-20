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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.operations.common.SocketBindingGroupIncludeAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingGroupIncludeRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model descriptions for socket-binding-group and socket-binding elements.
 *
 * @author Brian Stansberry
 */
public class SocketBindingGroupDescription {

    private static final String RESOURCE_NAME = SocketBindingGroupDescription.class.getPackage().getName() + ".LocalDescriptions";


    public static ModelNode getAddSocketBindingGroupIncludeOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(SocketBindingGroupIncludeAddHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("socket-binding-group.include.add"));
        root.get(REQUEST_PROPERTIES, INCLUDE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, INCLUDE, DESCRIPTION).set(bundle.getString("socket-binding-group.include.add.include"));
        root.get(REQUEST_PROPERTIES, INCLUDE, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, INCLUDE, MIN_LENGTH).set(1);
        root.get(REPLY_PROPERTIES).setEmptyObject();

        return root;
    }

    public static ModelNode getRemoveSocketBindingGroupIncludeOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(SocketBindingGroupIncludeRemoveHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("socket-binding-group.include.remove"));
        root.get(REQUEST_PROPERTIES, INCLUDE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, INCLUDE, DESCRIPTION).set(bundle.getString("socket-binding-group.include.remove.include"));
        root.get(REQUEST_PROPERTIES, INCLUDE, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, INCLUDE, MIN_LENGTH).set(1);
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
