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

package org.jboss.as.web;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * The web subsystem description providers.
 *
 * @author Emanuel Muckenhuber
 */
class WebSubsystemDescriptions {

    static final String RESOURCE_NAME = WebSubsystemDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    static ModelNode getSubsystemDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();

        node.get(DESCRIPTION).set(bundle.getString("web"));
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);
        node.get(NAMESPACE).set(Namespace.WEB_1_0.getUriString());

        node.get(ATTRIBUTES, Constants.DEFAULT_VIRTUAL_SERVER, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.DEFAULT_VIRTUAL_SERVER, DESCRIPTION).set(bundle.getString("web.default-virtual-server"));
        node.get(ATTRIBUTES, Constants.DEFAULT_VIRTUAL_SERVER, REQUIRED).set(false);

        node.get(ATTRIBUTES, Constants.CONTAINER_CONFIG, TYPE).set(ModelType.OBJECT);
        node.get(ATTRIBUTES, Constants.CONTAINER_CONFIG, DESCRIPTION).set(bundle.getString("web.configuration"));
        node.get(ATTRIBUTES, Constants.CONTAINER_CONFIG, REQUIRED).set(false);

        getConnectorCommonDescription(node.get(CHILDREN, Constants.CONNECTOR), bundle);
        getVirtualServerCommonDescription(node.get(CHILDREN, Constants.VIRTUAL_SERVER), bundle);

        return node;
    }

    static ModelNode getSubsystemAddDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("web.add"));

        node.get(REQUEST_PROPERTIES, Constants.DEFAULT_VIRTUAL_SERVER, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, Constants.DEFAULT_VIRTUAL_SERVER, DESCRIPTION).set(bundle.getString("web.default-virtual-server"));
        node.get(REQUEST_PROPERTIES, Constants.DEFAULT_VIRTUAL_SERVER, REQUIRED).set(false);

        node.get(REQUEST_PROPERTIES, Constants.CONTAINER_CONFIG, TYPE).set(ModelType.OBJECT);
        node.get(REQUEST_PROPERTIES, Constants.CONTAINER_CONFIG, DESCRIPTION).set(bundle.getString("web.configuration"));
        node.get(REQUEST_PROPERTIES, Constants.CONTAINER_CONFIG, REQUIRED).set(false);

        return node;
    }

    static ModelNode getConnectorDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);

        return getConnectorCommonDescription(node, bundle);
    }

    static ModelNode getConnectorCommonDescription(final ModelNode node, final ResourceBundle bundle) {

        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("web.connector"));

        node.get(ATTRIBUTES, Constants.NAME, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.NAME, DESCRIPTION).set(bundle.getString("web.connector.name"));
        node.get(ATTRIBUTES, Constants.NAME, REQUIRED).set(true);
        node.get(ATTRIBUTES, Constants.NAME, NILLABLE).set(false);

        node.get(ATTRIBUTES, Constants.PROTOCOL, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.PROTOCOL, DESCRIPTION).set(bundle.getString("web.connector.protocol"));
        node.get(ATTRIBUTES, Constants.PROTOCOL, REQUIRED).set(true);
        node.get(ATTRIBUTES, Constants.PROTOCOL, NILLABLE).set(false);

        node.get(ATTRIBUTES, Constants.SOCKET_BINDING, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.SOCKET_BINDING, DESCRIPTION).set(bundle.getString("web.connector.socket-binding"));
        node.get(ATTRIBUTES, Constants.SOCKET_BINDING, REQUIRED).set(true);
        node.get(ATTRIBUTES, Constants.SOCKET_BINDING, NILLABLE).set(false);

        node.get(ATTRIBUTES, Constants.SCHEME, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.SCHEME, DESCRIPTION).set(bundle.getString("web.connector.scheme"));
        node.get(ATTRIBUTES, Constants.SCHEME, REQUIRED).set(true);
        node.get(ATTRIBUTES, Constants.SCHEME, NILLABLE).set(false);

        for(final String metric : WebConnectorMetrics.ATTRIBUTES) {
            node.get(ATTRIBUTES, metric, TYPE).set(ModelType.INT);
        }

        return node;
    }

    static ModelNode getConnectorAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("web.connector.add"));

        node.get(REQUEST_PROPERTIES, Constants.PROTOCOL, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, Constants.PROTOCOL, DESCRIPTION).set(bundle.getString("web.connector.protocol"));
        node.get(REQUEST_PROPERTIES, Constants.PROTOCOL, REQUIRED).set(true);
        node.get(REQUEST_PROPERTIES, Constants.PROTOCOL, NILLABLE).set(false);

        node.get(REQUEST_PROPERTIES, Constants.SOCKET_BINDING, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, Constants.SOCKET_BINDING, DESCRIPTION).set(bundle.getString("web.connector.socket-binding"));
        node.get(REQUEST_PROPERTIES, Constants.SOCKET_BINDING, REQUIRED).set(true);
        node.get(REQUEST_PROPERTIES, Constants.SOCKET_BINDING, NILLABLE).set(false);

        node.get(REQUEST_PROPERTIES, Constants.SCHEME, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, Constants.SCHEME, DESCRIPTION).set(bundle.getString("web.connector.scheme"));
        node.get(REQUEST_PROPERTIES, Constants.SCHEME, REQUIRED).set(true);
        node.get(REQUEST_PROPERTIES, Constants.SCHEME, NILLABLE).set(false);

        return node;
    }

    static ModelNode getConnectorRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("web.connector.remove"));
        return node;
    }

    static ModelNode getVirtualServerDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();

        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);

        return getVirtualServerCommonDescription(node, bundle);
    }

    static ModelNode getVirtualServerCommonDescription(final ModelNode node, final ResourceBundle bundle) {
        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server"));

        node.get(ATTRIBUTES, Constants.NAME, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.NAME, DESCRIPTION).set(bundle.getString("web.virtual-server.name"));
        node.get(ATTRIBUTES, Constants.NAME, REQUIRED).set(true);
        node.get(ATTRIBUTES, Constants.NAME, NILLABLE).set(false);

        node.get(ATTRIBUTES, Constants.ALIAS, TYPE).set(ModelType.LIST);
        node.get(ATTRIBUTES, Constants.ALIAS, DESCRIPTION).set(bundle.getString("web.virtual-server.alias"));
        node.get(ATTRIBUTES, Constants.ALIAS, REQUIRED).set(false);
        node.get(ATTRIBUTES, Constants.ALIAS, NILLABLE).set(true);

        return node;
    }

    static ModelNode getVirtualServerAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.add"));

        node.get(REQUEST_PROPERTIES, Constants.ALIAS, TYPE).set(ModelType.LIST);
        node.get(REQUEST_PROPERTIES, Constants.ALIAS, DESCRIPTION).set(bundle.getString("web.virtual-server.alias"));
        node.get(REQUEST_PROPERTIES, Constants.ALIAS, REQUIRED).set(false);

        return node;
    }

    static ModelNode getVirtualServerRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.remove"));

        return node;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
