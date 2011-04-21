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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model descriptions for management protocol elements.
 *
 * @author Jason T. Greene
 */
public class ManagementDescription {

    private static final String RESOURCE_NAME = ManagementDescription.class.getPackage().getName() + ".LocalDescriptions";

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static ModelNode getNativeManagementDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("server.management.native-interface"));
        root.get(ATTRIBUTES, INTERFACE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, INTERFACE, DESCRIPTION).set(bundle.getString("server.management.native-interface.interface"));
        root.get(ATTRIBUTES, INTERFACE, REQUIRED).set(false);
        root.get(ATTRIBUTES, PORT, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PORT, DESCRIPTION).set(bundle.getString("server.management.native-interface.port"));
        root.get(ATTRIBUTES, PORT, REQUIRED).set(false);
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        return root;
    }

    public static ModelNode getHttpManagementDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("server.management.http-interface"));
        root.get(ATTRIBUTES, INTERFACE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, INTERFACE, DESCRIPTION).set(bundle.getString("server.management.http-interface.interface"));
        root.get(ATTRIBUTES, INTERFACE, REQUIRED).set(false);
        root.get(ATTRIBUTES, PORT, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PORT, DESCRIPTION).set(bundle.getString("server.management.http-interface.port"));
        root.get(ATTRIBUTES, PORT, REQUIRED).set(false);
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        return root;
    }

    public static ModelNode getAddNativeManagementDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode op = new ModelNode();
        op.get(DESCRIPTION).set(bundle.getString("server.management.native-interface.add"));
        op.get(REQUEST_PROPERTIES, INTERFACE, TYPE).set(ModelType.STRING);
        op.get(REQUEST_PROPERTIES, INTERFACE, DESCRIPTION).set(bundle.getString("server.management.native-interface.interface"));
        op.get(REQUEST_PROPERTIES, INTERFACE, REQUIRED).set(false);
        op.get(REQUEST_PROPERTIES, PORT, TYPE).set(ModelType.STRING);
        op.get(REQUEST_PROPERTIES, PORT, DESCRIPTION).set(bundle.getString("server.management.native-interface.port"));
        op.get(REQUEST_PROPERTIES, PORT, REQUIRED).set(false);

        return op;
    }

    public static ModelNode getAddHttpManagementDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode op = new ModelNode();
        op.get(DESCRIPTION).set(bundle.getString("server.management.http-interface.add"));
        op.get(REQUEST_PROPERTIES, INTERFACE, TYPE).set(ModelType.STRING);
        op.get(REQUEST_PROPERTIES, INTERFACE, DESCRIPTION).set(bundle.getString("server.management.http-interface.interface"));
        op.get(REQUEST_PROPERTIES, INTERFACE, REQUIRED).set(false);
        op.get(REQUEST_PROPERTIES, PORT, TYPE).set(ModelType.STRING);
        op.get(REQUEST_PROPERTIES, PORT, DESCRIPTION).set(bundle.getString("server.management.http-interface.port"));
        op.get(REQUEST_PROPERTIES, PORT, REQUIRED).set(false);

        return op;
    }
}
