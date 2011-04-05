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

        node.get(ATTRIBUTES, Constants.NATIVE, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.NATIVE, DESCRIPTION).set(bundle.getString("web.native"));
        node.get(ATTRIBUTES, Constants.NATIVE, REQUIRED).set(false);

        getConfigurationCommonDescription(node.get(ATTRIBUTES, Constants.CONTAINER_CONFIG), ATTRIBUTES, bundle);
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

        node.get(REQUEST_PROPERTIES, Constants.NATIVE, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, Constants.NATIVE, DESCRIPTION).set(bundle.getString("web.native"));
        node.get(REQUEST_PROPERTIES, Constants.NATIVE, REQUIRED).set(false);

        getConfigurationCommonDescription(node.get(REQUEST_PROPERTIES, Constants.CONTAINER_CONFIG), REQUEST_PROPERTIES, bundle);

        return node;
    }

    static ModelNode getConnectorDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);

        return getConnectorCommonDescription(node, bundle);
    }

    static ModelNode getConfigurationCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("web.configuration"));
        node.get(REQUIRED).set(false);

        getStaticResourcesCommonDescription(node.get(type, Constants.STATIC_RESOURCES), type, bundle);
        getJSPCommonDescription(node.get(type, Constants.STATIC_RESOURCES), type, bundle);

        node.get(type, Constants.MIME_MAPPING, TYPE).set(ModelType.STRING);
        node.get(type, Constants.MIME_MAPPING, DESCRIPTION).set(bundle.getString("web.configuration.mime-mapping"));
        node.get(type, Constants.MIME_MAPPING, REQUIRED).set(false);
        node.get(type, Constants.MIME_MAPPING, MAX_OCCURS).set(Integer.MAX_VALUE);

        node.get(type, Constants.MIME_MAPPING, Constants.NAME, TYPE).set(ModelType.STRING);
        node.get(type, Constants.MIME_MAPPING, Constants.NAME, DESCRIPTION).set(bundle.getString("web.configuration.mime-mapping.name"));
        node.get(type, Constants.MIME_MAPPING, Constants.NAME, REQUIRED).set(true);
        node.get(type, Constants.MIME_MAPPING, Constants.NAME, NILLABLE).set(false);

        node.get(type, Constants.MIME_MAPPING, Constants.VALUE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.MIME_MAPPING, Constants.VALUE, DESCRIPTION).set(bundle.getString("web.configuration.mime-mapping"));
        node.get(type, Constants.MIME_MAPPING, Constants.VALUE, REQUIRED).set(true);
        node.get(type, Constants.MIME_MAPPING, Constants.VALUE, NILLABLE).set(false);

        node.get(type, Constants.WELCOME_FILE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.WELCOME_FILE, DESCRIPTION).set(bundle.getString("web.configuration.welcome-file"));
        node.get(type, Constants.WELCOME_FILE, REQUIRED).set(false);
        node.get(type, Constants.WELCOME_FILE, NILLABLE).set(false);
        node.get(type, Constants.WELCOME_FILE, MAX_OCCURS).set(Integer.MAX_VALUE);

        return node;
    }

    static ModelNode getStaticResourcesCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("web.configuration.static"));
        node.get(REQUIRED).set(false);

        node.get(type, Constants.LISTINGS, TYPE).set(ModelType.STRING);
        node.get(type, Constants.LISTINGS, DESCRIPTION).set(bundle.getString("web.configuration.static.listings"));
        node.get(type, Constants.LISTINGS, REQUIRED).set(false);

        node.get(type, Constants.SENDFILE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SENDFILE, DESCRIPTION).set(bundle.getString("web.configuration.static.sendfile"));
        node.get(type, Constants.SENDFILE, REQUIRED).set(false);

        node.get(type, Constants.FILE_ENCONDING, TYPE).set(ModelType.STRING);
        node.get(type, Constants.FILE_ENCONDING, DESCRIPTION).set(bundle.getString("web.configuration.static.file-encoding"));
        node.get(type, Constants.FILE_ENCONDING, REQUIRED).set(false);

        node.get(type, Constants.READ_ONLY, TYPE).set(ModelType.STRING);
        node.get(type, Constants.READ_ONLY, DESCRIPTION).set(bundle.getString("web.configuration.static.read-only"));
        node.get(type, Constants.READ_ONLY, REQUIRED).set(false);

        node.get(type, Constants.WEBDAV, TYPE).set(ModelType.STRING);
        node.get(type, Constants.WEBDAV, DESCRIPTION).set(bundle.getString("web.configuration.static.webdav"));
        node.get(type, Constants.WEBDAV, REQUIRED).set(false);

        node.get(type, Constants.SECRET, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SECRET, DESCRIPTION).set(bundle.getString("web.configuration.static.secret"));
        node.get(type, Constants.SECRET, REQUIRED).set(false);

        node.get(type, Constants.MAX_DEPTH, TYPE).set(ModelType.STRING);
        node.get(type, Constants.MAX_DEPTH, DESCRIPTION).set(bundle.getString("web.configuration.static.max-depth"));
        node.get(type, Constants.MAX_DEPTH, REQUIRED).set(false);

        node.get(type, Constants.DISABLED, TYPE).set(ModelType.STRING);
        node.get(type, Constants.DISABLED, DESCRIPTION).set(bundle.getString("web.configuration.static.disabled"));
        node.get(type, Constants.DISABLED, REQUIRED).set(false);

        return node;
    }

    static ModelNode getJSPCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("web.configuration.jsp"));
        node.get(REQUIRED).set(false);

        node.get(type, Constants.DEVELOPMENT, TYPE).set(ModelType.STRING);
        node.get(type, Constants.DEVELOPMENT, DESCRIPTION).set(bundle.getString("web.configuration.jsp.development"));
        node.get(type, Constants.DEVELOPMENT, REQUIRED).set(false);

        node.get(type, Constants.DISABLED, TYPE).set(ModelType.STRING);
        node.get(type, Constants.DISABLED, DESCRIPTION).set(bundle.getString("web.configuration.jsp.disabled"));
        node.get(type, Constants.DISABLED, REQUIRED).set(false);

        node.get(type, Constants.KEEP_GENERATED, TYPE).set(ModelType.STRING);
        node.get(type, Constants.KEEP_GENERATED, DESCRIPTION).set(bundle.getString("web.configuration.jsp.keep-generated"));
        node.get(type, Constants.KEEP_GENERATED, REQUIRED).set(false);

        node.get(type, Constants.TRIM_SPACES, TYPE).set(ModelType.STRING);
        node.get(type, Constants.TRIM_SPACES, DESCRIPTION).set(bundle.getString("web.configuration.jsp.trim-spaces"));
        node.get(type, Constants.TRIM_SPACES, REQUIRED).set(false);

        node.get(type, Constants.TAG_POOLING, TYPE).set(ModelType.STRING);
        node.get(type, Constants.TAG_POOLING, DESCRIPTION).set(bundle.getString("web.configuration.jsp.tag-pooling"));
        node.get(type, Constants.TAG_POOLING, REQUIRED).set(false);

        node.get(type, Constants.MAPPED_FILE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.MAPPED_FILE, DESCRIPTION).set(bundle.getString("web.configuration.jsp.mapped-file"));
        node.get(type, Constants.MAPPED_FILE, REQUIRED).set(false);

        node.get(type, Constants.CHECK_INTERVAL, TYPE).set(ModelType.STRING);
        node.get(type, Constants.CHECK_INTERVAL, DESCRIPTION).set(bundle.getString("web.configuration.jsp.check-interval"));
        node.get(type, Constants.CHECK_INTERVAL, REQUIRED).set(false);

        node.get(type, Constants.MODIFIFICATION_TEST_INTERVAL, TYPE).set(ModelType.STRING);
        node.get(type, Constants.MODIFIFICATION_TEST_INTERVAL, DESCRIPTION).set(bundle.getString("web.configuration.jsp.modification-test-interval"));
        node.get(type, Constants.MODIFIFICATION_TEST_INTERVAL, REQUIRED).set(false);

        node.get(type, Constants.RECOMPILE_ON_FAIL, TYPE).set(ModelType.STRING);
        node.get(type, Constants.RECOMPILE_ON_FAIL, DESCRIPTION).set(bundle.getString("web.configuration.jsp.recompile-on-fail"));
        node.get(type, Constants.RECOMPILE_ON_FAIL, REQUIRED).set(false);

        node.get(type, Constants.SMAP, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SMAP, DESCRIPTION).set(bundle.getString("web.configuration.jsp.smap"));
        node.get(type, Constants.SMAP, REQUIRED).set(false);

        node.get(type, Constants.DUMP_SMAP, TYPE).set(ModelType.STRING);
        node.get(type, Constants.DUMP_SMAP, DESCRIPTION).set(bundle.getString("web.configuration.jsp.dump-smap"));
        node.get(type, Constants.DUMP_SMAP, REQUIRED).set(false);

        node.get(type, Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, TYPE).set(ModelType.STRING);
        node.get(type, Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, DESCRIPTION).set(bundle.getString("web.configuration.jsp.generate-strings-as-char-arrays"));
        node.get(type, Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, REQUIRED).set(false);

        node.get(type, Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, DESCRIPTION).set(bundle.getString("web.configuration.jsp.error-on-use-bean-invalid-class-attribute"));
        node.get(type, Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, REQUIRED).set(false);

        node.get(type, Constants.SCRATCH_DIR, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SCRATCH_DIR, DESCRIPTION).set(bundle.getString("web.configuration.jsp.scratch-dir"));
        node.get(type, Constants.SCRATCH_DIR, REQUIRED).set(false);

        node.get(type, Constants.SOURCE_VM, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SOURCE_VM, DESCRIPTION).set(bundle.getString("web.configuration.jsp.source-vm"));
        node.get(type, Constants.SOURCE_VM, REQUIRED).set(false);

        node.get(type, Constants.TARGET_VM, TYPE).set(ModelType.STRING);
        node.get(type, Constants.TARGET_VM, DESCRIPTION).set(bundle.getString("web.configuration.jsp.target-vm"));
        node.get(type, Constants.TARGET_VM, REQUIRED).set(false);

        node.get(type, Constants.JAVA_ENCODING, TYPE).set(ModelType.STRING);
        node.get(type, Constants.JAVA_ENCODING, DESCRIPTION).set(bundle.getString("web.configuration.jsp.java-encoding"));
        node.get(type, Constants.JAVA_ENCODING, REQUIRED).set(false);

        node.get(type, Constants.X_POWERED_BY, TYPE).set(ModelType.STRING);
        node.get(type, Constants.X_POWERED_BY, DESCRIPTION).set(bundle.getString("web.configuration.jsp.x-powered-by"));
        node.get(type, Constants.X_POWERED_BY, REQUIRED).set(false);

        node.get(type, Constants.DISPLAY_SOURCE_FRAGMENT, TYPE).set(ModelType.STRING);
        node.get(type, Constants.DISPLAY_SOURCE_FRAGMENT, DESCRIPTION).set(bundle.getString("web.configuration.jsp.display-source-fragment"));
        node.get(type, Constants.DISPLAY_SOURCE_FRAGMENT, REQUIRED).set(false);

        return node;
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
        node.get(ATTRIBUTES, Constants.PROTOCOL, REQUIRED).set(false);

        node.get(ATTRIBUTES, Constants.SOCKET_BINDING, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.SOCKET_BINDING, DESCRIPTION).set(bundle.getString("web.connector.socket-binding"));
        node.get(ATTRIBUTES, Constants.SOCKET_BINDING, REQUIRED).set(true);
        node.get(ATTRIBUTES, Constants.SOCKET_BINDING, NILLABLE).set(false);

        node.get(ATTRIBUTES, Constants.SCHEME, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.SCHEME, DESCRIPTION).set(bundle.getString("web.connector.scheme"));
        node.get(ATTRIBUTES, Constants.SCHEME, REQUIRED).set(false);

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
        node.get(REQUEST_PROPERTIES, Constants.PROTOCOL, REQUIRED).set(false);

        node.get(REQUEST_PROPERTIES, Constants.SOCKET_BINDING, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, Constants.SOCKET_BINDING, DESCRIPTION).set(bundle.getString("web.connector.socket-binding"));
        node.get(REQUEST_PROPERTIES, Constants.SOCKET_BINDING, REQUIRED).set(true);
        node.get(REQUEST_PROPERTIES, Constants.SOCKET_BINDING, NILLABLE).set(false);

        node.get(REQUEST_PROPERTIES, Constants.SCHEME, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, Constants.SCHEME, DESCRIPTION).set(bundle.getString("web.connector.scheme"));
        node.get(REQUEST_PROPERTIES, Constants.SCHEME, REQUIRED).set(false);

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
