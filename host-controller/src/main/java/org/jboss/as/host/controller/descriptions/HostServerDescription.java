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
package org.jboss.as.host.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class HostServerDescription {

    private static final String RESOURCE_NAME = HostServerDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getDescription(final Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("server"));
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(true);

        root.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("server.name"));
        root.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, GROUP, DESCRIPTION).set(bundle.getString("server.group"));
        root.get(ATTRIBUTES, GROUP, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, GROUP, REQUIRED).set(true);
        root.get(ATTRIBUTES, GROUP, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, AUTO_START, DESCRIPTION).set(bundle.getString("server.auto-start"));
        root.get(ATTRIBUTES, AUTO_START, TYPE).set(ModelType.BOOLEAN);
        root.get(ATTRIBUTES, AUTO_START, REQUIRED).set(true);

//        root.get(ATTRIBUTES, PRIORITY, DESCRIPTION).set(bundle.getString("server.priority"));
//        root.get(ATTRIBUTES, PRIORITY, TYPE).set(ModelType.INT);
//        root.get(ATTRIBUTES, PRIORITY, REQUIRED).set(false);
//        root.get(ATTRIBUTES, PRIORITY, MIN_VALUE).set(0);
//
//        root.get(ATTRIBUTES, CPU_AFFINITY, DESCRIPTION).set(bundle.getString("server.cpu-affinity"));
//        root.get(ATTRIBUTES, CPU_AFFINITY, TYPE).set(ModelType.STRING);
//        root.get(ATTRIBUTES, CPU_AFFINITY, REQUIRED).set(false);
//        root.get(ATTRIBUTES, CPU_AFFINITY, MIN_LENGTH).set(1);

        root.get(ATTRIBUTES, SOCKET_BINDING_GROUP, DESCRIPTION).set(bundle.getString("server.socket-binding-group"));
        root.get(ATTRIBUTES, SOCKET_BINDING_GROUP, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, SOCKET_BINDING_GROUP, REQUIRED).set(true);

        root.get(ATTRIBUTES, SOCKET_BINDING_PORT_OFFSET, DESCRIPTION).set(bundle.getString("server.socket-binding-port-offset"));
        root.get(ATTRIBUTES, SOCKET_BINDING_PORT_OFFSET, TYPE).set(ModelType.INT);
        root.get(ATTRIBUTES, SOCKET_BINDING_PORT_OFFSET, REQUIRED).set(false);

        root.get(OPERATIONS).setEmptyObject();

        root.get(CHILDREN, PATH, DESCRIPTION).set(bundle.getString("server.path"));
        root.get(CHILDREN, PATH, MIN_OCCURS).set(0);
        root.get(CHILDREN, PATH, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, PATH, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, SYSTEM_PROPERTY, DESCRIPTION).set(bundle.getString("server.system-properties"));
        root.get(CHILDREN, PATH, MIN_OCCURS).set(0);
        root.get(CHILDREN, PATH, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, PATH, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, INTERFACE, DESCRIPTION).set(bundle.getString("server.interface"));
        root.get(CHILDREN, INTERFACE, MIN_OCCURS).set(0);
        root.get(CHILDREN, INTERFACE, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, INTERFACE, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, JVM, DESCRIPTION).set(bundle.getString("server.jvm"));
        root.get(CHILDREN, JVM, MIN_OCCURS).set(0);
        root.get(CHILDREN, JVM, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, JVM, MODEL_DESCRIPTION).setEmptyObject();

        return root;
    }

    private static void populateServer(ModelNode root, ResourceBundle bundle, boolean specified) {
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        //TODO
        root.get(OPERATIONS).setEmptyObject();
    }

    public static ModelNode getServerAddOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();

        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("server.add"));

        root.get(REQUEST_PROPERTIES, GROUP, DESCRIPTION).set(bundle.getString("server.group"));
        root.get(REQUEST_PROPERTIES, GROUP, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, GROUP, REQUIRED).set(true);

        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_GROUP, DESCRIPTION).set(bundle.getString("server.socket-binding-group"));
        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_GROUP, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_GROUP, REQUIRED).set(false);

        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_PORT_OFFSET, DESCRIPTION).set(bundle.getString("server.socket-binding-port-offset"));
        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_PORT_OFFSET, TYPE).set(ModelType.INT);
        root.get(REQUEST_PROPERTIES, SOCKET_BINDING_PORT_OFFSET, REQUIRED).set(false);

        root.get(REQUEST_PROPERTIES, AUTO_START, DESCRIPTION).set(bundle.getString("server.auto-start"));
        root.get(REQUEST_PROPERTIES, AUTO_START, TYPE).set(ModelType.BOOLEAN);
        root.get(REQUEST_PROPERTIES, AUTO_START, REQUIRED).set(false);

        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getServerRemoveOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(REMOVE);
        root.get(DESCRIPTION).set(bundle.getString("server.remove"));

        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static ModelNode getSystemPropertiesDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return CommonDescriptions.getSystemPropertyDescription(locale, bundle.getString("server.system-property"), true);
    }
}
