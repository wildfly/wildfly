/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADMIN_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DUMP_SERVICES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SHUTDOWN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.ProcessReloadHandler;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model descriptions for deployment resources.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@Deprecated
public class ServerDescriptions {

    public static final String RESOURCE_NAME = ServerDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0) {
                prefix.append('.').append(kp);
            } else {
                prefix.append(kp);
            }
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, ServerDescriptions.class.getClassLoader(), true, true);
    }

    private ServerDescriptions() {
    }

    public static final ModelNode getServerReloadOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return CommonDescriptions.getSingleParamOnlyOperation(bundle, ProcessReloadHandler.OPERATION_NAME, null,
                ADMIN_ONLY, ModelType.BOOLEAN, true);
    }

    public static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix, final boolean useUnprefixedChildTypes) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, ServerDescriptions.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    public static ModelNode getCompositeOperationDescription(Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(COMPOSITE);
        root.get(DESCRIPTION).set(bundle.getString("composite"));
        root.get(REQUEST_PROPERTIES, STEPS, TYPE).set(ModelType.LIST); // TODO details of the type
        root.get(REQUEST_PROPERTIES, STEPS, DESCRIPTION).set(bundle.getString("composite.steps"));
        root.get(REQUEST_PROPERTIES, STEPS, REQUIRED).set(true);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
        // TODO details of the reply
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("composite.result"));
        return root;
    }

    /** {@inheritDoc} */
    public static ModelNode getShutdownOperationDescription(final Locale locale) {
        ResourceBundle bundle = getResourceBundle(locale);

        ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(SHUTDOWN);
        node.get(DESCRIPTION).set(bundle.getString("shutdown"));
        node.get(REQUEST_PROPERTIES, RESTART, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, RESTART, DESCRIPTION).set(bundle.getString("shutdown.restart"));
        node.get(REQUEST_PROPERTIES, RESTART, DEFAULT).set(false);
        node.get(REQUEST_PROPERTIES, RESTART, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, RESTART, NILLABLE).set(true);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    public static ModelNode getDumpServicesOperationDescription(final Locale locale) {
        ResourceBundle bundle = getResourceBundle(locale);

        ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(DUMP_SERVICES);
        node.get(DESCRIPTION).set(bundle.getString("dump-services"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        return node;
    }

    public static ModelNode getRestartRequiredDescription(final Locale locale) {
        ResourceBundle bundle = getResourceBundle(locale);

        ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ServerRestartRequiredHandler.OPERATION_NAME);
        node.get(DESCRIPTION).set(bundle.getString("restart-required"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }
}
