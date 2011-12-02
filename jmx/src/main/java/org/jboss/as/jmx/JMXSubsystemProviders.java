/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.jmx.CommonAttributes.*;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
public class JMXSubsystemProviders {

    static final String RESOURCE_NAME = JMXSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("jmx"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.JMX_1_0.getUriString());

            subsystem.get(ATTRIBUTES, REGISTRY_BINDING, DESCRIPTION).set(bundle.getString("registry.binding"));
            subsystem.get(ATTRIBUTES, REGISTRY_BINDING, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, REGISTRY_BINDING, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, REGISTRY_BINDING, MIN_OCCURS).set(1);
            subsystem.get(ATTRIBUTES, REGISTRY_BINDING, MAX_OCCURS).set(1);

            subsystem.get(ATTRIBUTES, SERVER_BINDING, DESCRIPTION).set(bundle.getString("server.binding"));
            subsystem.get(ATTRIBUTES, SERVER_BINDING, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, SERVER_BINDING, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, SERVER_BINDING, MIN_OCCURS).set(1);
            subsystem.get(ATTRIBUTES, SERVER_BINDING, MAX_OCCURS).set(1);

            subsystem.get(ATTRIBUTES, PASSWORD_FILE, DESCRIPTION).set(bundle.getString("password.file"));
            subsystem.get(ATTRIBUTES, PASSWORD_FILE, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, PASSWORD_FILE, REQUIRED).set(false);

            subsystem.get(ATTRIBUTES, ACCESS_FILE, DESCRIPTION).set(bundle.getString("access.file"));
            subsystem.get(ATTRIBUTES, ACCESS_FILE, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, ACCESS_FILE, REQUIRED).set(false);

            return subsystem;
        }
    };

    static final DescriptionProvider SUBSYTEM_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OPERATION_NAME).set(ADD);
            subsystem.get(DESCRIPTION).set(bundle.getString("jmx.add"));

            return subsystem;
        }

    };

    static final DescriptionProvider JMX_CONNECTOR_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(JMXConnectorAdd.OPERATION_NAME);
            op.get(DESCRIPTION).set(bundle.getString("jmx.connector.add"));

            op.get(REQUEST_PROPERTIES, REGISTRY_BINDING, DESCRIPTION).set(bundle.getString("registry.binding"));
            op.get(REQUEST_PROPERTIES, REGISTRY_BINDING, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, REGISTRY_BINDING, REQUIRED).set(true);
            op.get(REQUEST_PROPERTIES, REGISTRY_BINDING, MIN_OCCURS).set(1);
            op.get(REQUEST_PROPERTIES, REGISTRY_BINDING, MAX_OCCURS).set(1);

            op.get(REQUEST_PROPERTIES, SERVER_BINDING, DESCRIPTION).set(bundle.getString("server.binding"));
            op.get(REQUEST_PROPERTIES, SERVER_BINDING, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, SERVER_BINDING, REQUIRED).set(true);
            op.get(REQUEST_PROPERTIES, SERVER_BINDING, MIN_OCCURS).set(1);
            op.get(REQUEST_PROPERTIES, SERVER_BINDING, MAX_OCCURS).set(1);

            op.get(REQUEST_PROPERTIES, PASSWORD_FILE, DESCRIPTION).set(bundle.getString("password.file"));
            op.get(REQUEST_PROPERTIES, PASSWORD_FILE, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, PASSWORD_FILE, REQUIRED).set(false);

            op.get(REQUEST_PROPERTIES, ACCESS_FILE, DESCRIPTION).set(bundle.getString("password.file"));
            op.get(REQUEST_PROPERTIES, ACCESS_FILE, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, ACCESS_FILE, REQUIRED).set(false);

            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }
    };

    static final DescriptionProvider JMX_CONNECTOR_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(JMXConnectorRemove.OPERATION_NAME);
            op.get(DESCRIPTION).set(bundle.getString("jmx.connector.remove"));

            op.get(REQUEST_PROPERTIES).setEmptyObject();

            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }

    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
