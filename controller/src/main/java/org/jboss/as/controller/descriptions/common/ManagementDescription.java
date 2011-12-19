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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTBOUND_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;

/**
 * Model description for the management elements.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ManagementDescription {

    private static final String RESOURCE_NAME = ManagementDescription.class.getPackage().getName() + ".LocalDescriptions";

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static ModelNode getManagementDescriptionWithInterfaces(final Locale locale) {
        return getManagementDescription(locale, true);
    }

    public static ModelNode getManagementDescription(final Locale locale, boolean interfaces) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("core.management"));
        root.get(OPERATIONS).setEmptyObject();

        root.get(CHILDREN, SECURITY_REALM, DESCRIPTION).set(bundle.getString("core.management.security-realms"));
        root.get(CHILDREN, SECURITY_REALM, MIN_OCCURS).set(0);
        root.get(CHILDREN, SECURITY_REALM, MODEL_DESCRIPTION);

        root.get(CHILDREN, OUTBOUND_CONNECTION, DESCRIPTION).set(bundle.getString("core.management.outbound-connections"));
        root.get(CHILDREN, OUTBOUND_CONNECTION, MIN_OCCURS).set(0);
        root.get(CHILDREN, OUTBOUND_CONNECTION, MODEL_DESCRIPTION);

        if (interfaces) {
            root.get(CHILDREN, MANAGEMENT_INTERFACE, DESCRIPTION).set(bundle.getString("core.management.management-interfaces"));
            root.get(CHILDREN, MANAGEMENT_INTERFACE, MIN_OCCURS).set(0);
            root.get(CHILDREN, MANAGEMENT_INTERFACE, MODEL_DESCRIPTION);
        }

        return root;
    }

    public static ModelNode getManagementSecurityRealmDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("core.management.security-realm"));
        // TODO attributes
        root.get(OPERATIONS).setEmptyObject();
        return root;
    }

    public static ModelNode getAddManagementSecurityRealmDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("core.management.security-realm.add"));
        //TODO attributes
        return root;
    }

    public static ModelNode getManagementOutboundConnectionDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("core.management.outbound-connection"));
        // TODO attributes
        root.get(OPERATIONS).setEmptyObject();
        return root;
    }

    public static ModelNode getAddManagementOutboundConnectionDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("core.management.outbound-connection"));
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("core.management.outbound-connection.add"));
        // TODO attributes
        return root;
    }
}
