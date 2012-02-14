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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * The web subsystem description providers.
 *
 * @author Emanuel Muckenhuber
 * @author Jean-Frederic Clere
 */
class WebSubsystemDescriptions {

    static final String RESOURCE_NAME = WebSubsystemDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    static ModelNode getSubsystemDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();

        node.get(DESCRIPTION).set(bundle.getString("web"));
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);
        node.get(NAMESPACE).set(Namespace.WEB_1_1.getUriString());

        node.get(ATTRIBUTES, Constants.DEFAULT_VIRTUAL_SERVER, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.DEFAULT_VIRTUAL_SERVER, DESCRIPTION).set(bundle.getString("web.default-virtual-server"));
        node.get(ATTRIBUTES, Constants.DEFAULT_VIRTUAL_SERVER, REQUIRED).set(false);
        node.get(ATTRIBUTES, Constants.DEFAULT_VIRTUAL_SERVER, DEFAULT).set("localhost");

        node.get(ATTRIBUTES, Constants.NATIVE, TYPE).set(ModelType.BOOLEAN);
        node.get(ATTRIBUTES, Constants.NATIVE, DESCRIPTION).set(bundle.getString("web.native"));
        node.get(ATTRIBUTES, Constants.NATIVE, REQUIRED).set(false);
        node.get(ATTRIBUTES, Constants.NATIVE, DEFAULT).set(true);

        node.get(ATTRIBUTES, Constants.INSTANCE_ID, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, Constants.INSTANCE_ID, DESCRIPTION).set(bundle.getString("web.instance-id"));
        node.get(ATTRIBUTES, Constants.INSTANCE_ID, REQUIRED).set(false);

        node.get(CHILDREN, Constants.CONTAINER_CONFIG, DESCRIPTION).set(bundle.getString("web.configuration"));
        node.get(CHILDREN, Constants.CONTAINER_CONFIG, MODEL_DESCRIPTION).setEmptyObject();

        node.get(CHILDREN, Constants.CONNECTOR, DESCRIPTION).set(bundle.getString("web.connector"));
        node.get(CHILDREN, Constants.CONNECTOR, MODEL_DESCRIPTION);
        node.get(CHILDREN, Constants.VIRTUAL_SERVER, DESCRIPTION).set(bundle.getString("web.virtual-server"));
        node.get(CHILDREN, Constants.VIRTUAL_SERVER, MODEL_DESCRIPTION);


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
        node.get(REQUEST_PROPERTIES, Constants.DEFAULT_VIRTUAL_SERVER, DEFAULT).set("localhost");

        node.get(REQUEST_PROPERTIES, Constants.NATIVE, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, Constants.NATIVE, DESCRIPTION).set(bundle.getString("web.native"));
        node.get(REQUEST_PROPERTIES, Constants.NATIVE, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, Constants.NATIVE, DEFAULT).set(true);

        node.get(REQUEST_PROPERTIES, Constants.INSTANCE_ID, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, Constants.INSTANCE_ID, DESCRIPTION).set(bundle.getString("web.instance-id"));
        node.get(REQUEST_PROPERTIES, Constants.INSTANCE_ID, REQUIRED).set(false);

        // Add configuration, connectors etc
        ModelNode configuration = node.get(REQUEST_PROPERTIES, Constants.CONTAINER_CONFIG);
        getConfigurationCommonDescription(configuration, "value-type", bundle);
        configuration.get(TYPE).set(ModelType.OBJECT);
        configuration.get(REQUIRED).set(false);

        ModelNode connector = node.get(REQUEST_PROPERTIES, Constants.CONNECTOR);
        getConnectorCommonDescription(connector, "value-type", bundle);
        connector.get(TYPE).set(ModelType.OBJECT);
        connector.get(REQUIRED).set(false);

        ModelNode virtual = node.get(REQUEST_PROPERTIES,  Constants.VIRTUAL_SERVER);
        getVirtualServerCommonDescription(virtual, "value-type", bundle);
        virtual.get(TYPE).set(ModelType.OBJECT);
        virtual.get(REQUIRED).set(false);

        return node;
    }

    public static ModelNode getSubsystemRemoveDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("web.remove"));
        op.get(REPLY_PROPERTIES).setEmptyObject();
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getConnectorDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        ModelNode node = new ModelNode();
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);

        return getConnectorCommonDescription(node, ATTRIBUTES, bundle);
    }

    static ModelNode getConfigurationCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(DESCRIPTION).set(bundle.getString("web.configuration"));
        if (ATTRIBUTES.equals(type)) {
            getStaticResourceDescription(node.get(CHILDREN, Constants.STATIC_RESOURCES), type, bundle);
            getJspConfigurationDescription(node.get(CHILDREN, Constants.JSP_CONFIGURATION), type, bundle);

            final ModelNode mime = node.get(CHILDREN, Constants.MIME_MAPPING);
            mime.get(DESCRIPTION).set(bundle.getString("web.configuration.mime-mapping"));
            mime.get(REQUIRED).set(false);
            mime.get(MAX_OCCURS).set(Integer.MAX_VALUE);

            mime.get(type, Constants.NAME, TYPE).set(ModelType.STRING);
            mime.get(type, Constants.NAME, DESCRIPTION).set(bundle.getString("web.configuration.mime-mapping.name"));
            mime.get(type, Constants.NAME, REQUIRED).set(true);
            mime.get(type, Constants.NAME, NILLABLE).set(false);

            mime.get(type, Constants.VALUE, TYPE).set(ModelType.STRING);
            mime.get(type, Constants.VALUE, DESCRIPTION).set(bundle.getString("web.configuration.mime-mapping.value"));
            mime.get(type, Constants.VALUE, REQUIRED).set(true);
            mime.get(type, Constants.VALUE, NILLABLE).set(false);
        }

        node.get(type, Constants.WELCOME_FILE, TYPE).set(ModelType.LIST);
        node.get(type, Constants.WELCOME_FILE, VALUE_TYPE).set(ModelType.STRING);
        node.get(type, Constants.WELCOME_FILE, DESCRIPTION).set(bundle.getString("web.configuration.welcome-file"));
        node.get(type, Constants.WELCOME_FILE, REQUIRED).set(false);
        node.get(type, Constants.WELCOME_FILE, NILLABLE).set(true);
        // TODO node.get(type, Constants.WELCOME_FILE, MAX_OCCURS).set(Integer.MAX_VALUE);

        return node;
    }

    static ModelNode getStaticResourcesCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(type, Constants.LISTINGS, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.LISTINGS, DESCRIPTION).set(bundle.getString("web.configuration.static.listings"));
        node.get(type, Constants.LISTINGS, REQUIRED).set(false);
        node.get(type, Constants.LISTINGS, DEFAULT).set(false);

        node.get(type, Constants.SENDFILE, TYPE).set(ModelType.INT);
        node.get(type, Constants.SENDFILE, DESCRIPTION).set(bundle.getString("web.configuration.static.sendfile"));
        node.get(type, Constants.SENDFILE, REQUIRED).set(false);
        node.get(type, Constants.SENDFILE, DEFAULT).set(49152);

        node.get(type, Constants.FILE_ENCODING, TYPE).set(ModelType.STRING);
        node.get(type, Constants.FILE_ENCODING, DESCRIPTION).set(bundle.getString("web.configuration.static.file-encoding"));
        node.get(type, Constants.FILE_ENCODING, REQUIRED).set(false);

        node.get(type, Constants.READ_ONLY, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.READ_ONLY, DESCRIPTION).set(bundle.getString("web.configuration.static.read-only"));
        node.get(type, Constants.READ_ONLY, REQUIRED).set(false);
        node.get(type, Constants.READ_ONLY, DEFAULT).set(true);

        node.get(type, Constants.WEBDAV, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.WEBDAV, DESCRIPTION).set(bundle.getString("web.configuration.static.webdav"));
        node.get(type, Constants.WEBDAV, REQUIRED).set(false);
        node.get(type, Constants.WEBDAV, DEFAULT).set(false);

        node.get(type, Constants.SECRET, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SECRET, DESCRIPTION).set(bundle.getString("web.configuration.static.secret"));
        node.get(type, Constants.SECRET, REQUIRED).set(false);

        node.get(type, Constants.MAX_DEPTH, TYPE).set(ModelType.INT);
        node.get(type, Constants.MAX_DEPTH, DESCRIPTION).set(bundle.getString("web.configuration.static.max-depth"));
        node.get(type, Constants.MAX_DEPTH, REQUIRED).set(false);
        node.get(type, Constants.MAX_DEPTH, DEFAULT).set(3);

        node.get(type, Constants.DISABLED, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.DISABLED, DESCRIPTION).set(bundle.getString("web.configuration.static.disabled"));
        node.get(type, Constants.DISABLED, REQUIRED).set(false);
        node.get(type, Constants.DISABLED, DEFAULT).set(false);

        return node;
    }

    static ModelNode getJSPCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(DESCRIPTION).set(bundle.getString("web.configuration.jsp"));

        node.get(type, Constants.DEVELOPMENT, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.DEVELOPMENT, DESCRIPTION).set(bundle.getString("web.configuration.jsp.development"));
        node.get(type, Constants.DEVELOPMENT, REQUIRED).set(false);
        node.get(type, Constants.DEVELOPMENT, DEFAULT).set(false);

        node.get(type, Constants.DISABLED, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.DISABLED, DESCRIPTION).set(bundle.getString("web.configuration.jsp.disabled"));
        node.get(type, Constants.DISABLED, REQUIRED).set(false);
        node.get(type, Constants.DISABLED, DEFAULT).set(false);

        node.get(type, Constants.KEEP_GENERATED, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.KEEP_GENERATED, DESCRIPTION).set(bundle.getString("web.configuration.jsp.keep-generated"));
        node.get(type, Constants.KEEP_GENERATED, REQUIRED).set(false);
        node.get(type, Constants.KEEP_GENERATED, DEFAULT).set(true);

        node.get(type, Constants.TRIM_SPACES, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.TRIM_SPACES, DESCRIPTION).set(bundle.getString("web.configuration.jsp.trim-spaces"));
        node.get(type, Constants.TRIM_SPACES, REQUIRED).set(false);
        node.get(type, Constants.TRIM_SPACES, DEFAULT).set(true);

        node.get(type, Constants.TAG_POOLING, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.TAG_POOLING, DESCRIPTION).set(bundle.getString("web.configuration.jsp.tag-pooling"));
        node.get(type, Constants.TAG_POOLING, REQUIRED).set(false);
        node.get(type, Constants.TAG_POOLING, DEFAULT).set(true);

        node.get(type, Constants.MAPPED_FILE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.MAPPED_FILE, DESCRIPTION).set(bundle.getString("web.configuration.jsp.mapped-file"));
        node.get(type, Constants.MAPPED_FILE, REQUIRED).set(false);
        node.get(type, Constants.MAPPED_FILE, DEFAULT).set(true);

        node.get(type, Constants.CHECK_INTERVAL, TYPE).set(ModelType.INT);
        node.get(type, Constants.CHECK_INTERVAL, DESCRIPTION).set(bundle.getString("web.configuration.jsp.check-interval"));
        node.get(type, Constants.CHECK_INTERVAL, REQUIRED).set(false);
        node.get(type, Constants.CHECK_INTERVAL, DEFAULT).set(0);

        node.get(type, Constants.MODIFICATION_TEST_INTERVAL, TYPE).set(ModelType.INT);
        node.get(type, Constants.MODIFICATION_TEST_INTERVAL, DESCRIPTION).set(bundle.getString("web.configuration.jsp.modification-test-interval"));
        node.get(type, Constants.MODIFICATION_TEST_INTERVAL, REQUIRED).set(false);
        node.get(type, Constants.MODIFICATION_TEST_INTERVAL, DEFAULT).set(4);

        node.get(type, Constants.RECOMPILE_ON_FAIL, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.RECOMPILE_ON_FAIL, DESCRIPTION).set(bundle.getString("web.configuration.jsp.recompile-on-fail"));
        node.get(type, Constants.RECOMPILE_ON_FAIL, REQUIRED).set(false);
        node.get(type, Constants.RECOMPILE_ON_FAIL, DEFAULT).set(false);

        node.get(type, Constants.SMAP, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.SMAP, DESCRIPTION).set(bundle.getString("web.configuration.jsp.smap"));
        node.get(type, Constants.SMAP, REQUIRED).set(false);
        node.get(type, Constants.SMAP, DEFAULT).set(false);

        node.get(type, Constants.DUMP_SMAP, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.DUMP_SMAP, DESCRIPTION).set(bundle.getString("web.configuration.jsp.dump-smap"));
        node.get(type, Constants.DUMP_SMAP, REQUIRED).set(false);
        node.get(type, Constants.DUMP_SMAP, DEFAULT).set(false);

        node.get(type, Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, DESCRIPTION).set(bundle.getString("web.configuration.jsp.generate-strings-as-char-arrays"));
        node.get(type, Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, REQUIRED).set(false);
        node.get(type, Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, DEFAULT).set(false);

        node.get(type, Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, DESCRIPTION).set(bundle.getString("web.configuration.jsp.error-on-use-bean-invalid-class-attribute"));
        node.get(type, Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, REQUIRED).set(false);
        node.get(type, Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, DEFAULT).set(false);

        node.get(type, Constants.SCRATCH_DIR, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SCRATCH_DIR, DESCRIPTION).set(bundle.getString("web.configuration.jsp.scratch-dir"));
        node.get(type, Constants.SCRATCH_DIR, REQUIRED).set(false);

        node.get(type, Constants.SOURCE_VM, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SOURCE_VM, DESCRIPTION).set(bundle.getString("web.configuration.jsp.source-vm"));
        node.get(type, Constants.SOURCE_VM, REQUIRED).set(false);
        node.get(type, Constants.SOURCE_VM, DEFAULT).set("1.5");

        node.get(type, Constants.TARGET_VM, TYPE).set(ModelType.STRING);
        node.get(type, Constants.TARGET_VM, DESCRIPTION).set(bundle.getString("web.configuration.jsp.target-vm"));
        node.get(type, Constants.TARGET_VM, REQUIRED).set(false);
        node.get(type, Constants.TARGET_VM, DEFAULT).set("1.5");

        node.get(type, Constants.JAVA_ENCODING, TYPE).set(ModelType.STRING);
        node.get(type, Constants.JAVA_ENCODING, DESCRIPTION).set(bundle.getString("web.configuration.jsp.java-encoding"));
        node.get(type, Constants.JAVA_ENCODING, REQUIRED).set(false);
        node.get(type, Constants.JAVA_ENCODING, DEFAULT).set("UTF-8");

        node.get(type, Constants.X_POWERED_BY, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.X_POWERED_BY, DESCRIPTION).set(bundle.getString("web.configuration.jsp.x-powered-by"));
        node.get(type, Constants.X_POWERED_BY, REQUIRED).set(false);
        node.get(type, Constants.X_POWERED_BY, DEFAULT).set(true);

        node.get(type, Constants.DISPLAY_SOURCE_FRAGMENT, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.DISPLAY_SOURCE_FRAGMENT, DESCRIPTION).set(bundle.getString("web.configuration.jsp.display-source-fragment"));
        node.get(type, Constants.DISPLAY_SOURCE_FRAGMENT, REQUIRED).set(false);
        node.get(type, Constants.DISPLAY_SOURCE_FRAGMENT, DEFAULT).set(true);

        return node;
    }

    static ModelNode getConnectorCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(DESCRIPTION).set(bundle.getString("web.connector"));

        node.get(type, Constants.NAME, TYPE).set(ModelType.STRING);
        node.get(type, Constants.NAME, DESCRIPTION).set(bundle.getString("web.connector.name"));
        node.get(type, Constants.NAME, REQUIRED).set(false); // TODO should be true.
        node.get(type, Constants.NAME, NILLABLE).set(false);

        node.get(type, Constants.PROTOCOL, TYPE).set(ModelType.STRING);
        node.get(type, Constants.PROTOCOL, DESCRIPTION).set(bundle.getString("web.connector.protocol"));
        node.get(type, Constants.PROTOCOL, REQUIRED).set(true);

        node.get(type, Constants.SOCKET_BINDING, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SOCKET_BINDING, DESCRIPTION).set(bundle.getString("web.connector.socket-binding"));
        node.get(type, Constants.SOCKET_BINDING, REQUIRED).set(true);
        node.get(type, Constants.SOCKET_BINDING, NILLABLE).set(false);

        node.get(type, Constants.SCHEME, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SCHEME, DESCRIPTION).set(bundle.getString("web.connector.scheme"));
        node.get(type, Constants.SCHEME, REQUIRED).set(false);
        node.get(type, Constants.SCHEME, DEFAULT).set("http");

        node.get(type, Constants.EXECUTOR, TYPE).set(ModelType.STRING);
        node.get(type, Constants.EXECUTOR, DESCRIPTION).set(bundle.getString("web.connector.executor"));
        node.get(type, Constants.EXECUTOR, REQUIRED).set(false);

        node.get(type, Constants.ENABLED, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.ENABLED, DESCRIPTION).set(bundle.getString("web.connector.enabled"));
        node.get(type, Constants.ENABLED, REQUIRED).set(false);
        node.get(type, Constants.ENABLED, DEFAULT).set(true);

        node.get(type, Constants.ENABLE_LOOKUPS, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.ENABLE_LOOKUPS, DESCRIPTION).set(bundle.getString("web.connector.enable-lookups"));
        node.get(type, Constants.ENABLE_LOOKUPS, REQUIRED).set(false);
        node.get(type, Constants.ENABLE_LOOKUPS, DEFAULT).set(false);

        node.get(type, Constants.PROXY_NAME, TYPE).set(ModelType.STRING);
        node.get(type, Constants.PROXY_NAME, DESCRIPTION).set(bundle.getString("web.connector.proxy-name"));
        node.get(type, Constants.PROXY_NAME, REQUIRED).set(false);

        node.get(type, Constants.PROXY_PORT, TYPE).set(ModelType.INT);
        node.get(type, Constants.PROXY_PORT, DESCRIPTION).set(bundle.getString("web.connector.proxy-port"));
        node.get(type, Constants.PROXY_PORT, REQUIRED).set(false);

        node.get(type, Constants.MAX_POST_SIZE, TYPE).set(ModelType.INT);
        node.get(type, Constants.MAX_POST_SIZE, DESCRIPTION).set(bundle.getString("web.connector.max-post-size"));
        node.get(type, Constants.MAX_POST_SIZE, REQUIRED).set(false);
        node.get(type, Constants.MAX_POST_SIZE, DEFAULT).set(2097152);

        node.get(type, Constants.MAX_SAVE_POST_SIZE, TYPE).set(ModelType.INT);
        node.get(type, Constants.MAX_SAVE_POST_SIZE, DESCRIPTION).set(bundle.getString("web.connector.max-save-post-size"));
        node.get(type, Constants.MAX_SAVE_POST_SIZE, REQUIRED).set(false);
        node.get(type, Constants.MAX_SAVE_POST_SIZE, DEFAULT).set(4096);

        node.get(type, Constants.SECURE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.SECURE, DESCRIPTION).set(bundle.getString("web.connector.secure"));
        node.get(type, Constants.SECURE, REQUIRED).set(false);
        node.get(type, Constants.SECURE, DEFAULT).set(false);

        node.get(type, Constants.REDIRECT_PORT, TYPE).set(ModelType.INT);
        node.get(type, Constants.REDIRECT_PORT, DESCRIPTION).set(bundle.getString("web.connector.redirect-port"));
        node.get(type, Constants.REDIRECT_PORT, REQUIRED).set(false);
        node.get(type, Constants.REDIRECT_PORT, DEFAULT).set(8443);

        node.get(type, Constants.MAX_CONNECTIONS, TYPE).set(ModelType.INT);
        node.get(type, Constants.MAX_CONNECTIONS, DESCRIPTION).set(bundle.getString("web.connector.max-connections"));
        node.get(type, Constants.MAX_CONNECTIONS, REQUIRED).set(false);

        node.get(type, Constants.VIRTUAL_SERVER, TYPE).set(ModelType.LIST);
        node.get(type, Constants.VIRTUAL_SERVER, VALUE_TYPE).set(ModelType.STRING);
        node.get(type, Constants.VIRTUAL_SERVER, DESCRIPTION).set(bundle.getString("web.connector.virtual-server"));
        node.get(type, Constants.VIRTUAL_SERVER, REQUIRED).set(false);

        if (ATTRIBUTES.equals(type)) {
            /* add the stats descriptions */

            node.get(type, Constants.BYTES_SENT, TYPE).set(ModelType.INT);
            node.get(type, Constants.BYTES_SENT, DESCRIPTION).set(bundle.getString("web.connector.stats.bytes-sent"));
            node.get(type, Constants.BYTES_RECEIVED, TYPE).set(ModelType.INT);
            node.get(type, Constants.BYTES_RECEIVED, DESCRIPTION).set(bundle.getString("web.connector.stats.bytes-received"));
            node.get(type, Constants.PROCESSING_TIME, TYPE).set(ModelType.INT);
            node.get(type, Constants.PROCESSING_TIME, DESCRIPTION).set(bundle.getString("web.connector.stats.processing-time"));
            node.get(type, Constants.ERROR_COUNT, TYPE).set(ModelType.INT);
            node.get(type, Constants.ERROR_COUNT, DESCRIPTION).set(bundle.getString("web.connector.stats.error-count"));
            node.get(type, Constants.MAX_TIME, TYPE).set(ModelType.INT);
            node.get(type, Constants.MAX_TIME, DESCRIPTION).set(bundle.getString("web.connector.stats.max-time"));
            node.get(type, Constants.REQUEST_COUNT, TYPE).set(ModelType.INT);
            node.get(type, Constants.REQUEST_COUNT, DESCRIPTION).set(bundle.getString("web.connector.request-count"));

            /* add the ssl descriptions */
            node.get(CHILDREN, Constants.SSL, DESCRIPTION).set(bundle.getString("web.connector.ssl"));
            node.get(CHILDREN, Constants.SSL, MODEL_DESCRIPTION).setEmptyObject();
        }

        return node;
    }

    static ModelNode addSSLCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(type, Constants.NAME, TYPE).set(ModelType.STRING);
        node.get(type, Constants.NAME, DESCRIPTION).set(bundle.getString("web.connector.ssl.name"));
        node.get(type, Constants.NAME, REQUIRED).set(false);

        node.get(type, Constants.KEY_ALIAS, TYPE).set(ModelType.STRING);
        node.get(type, Constants.KEY_ALIAS, DESCRIPTION).set(bundle.getString("web.connector.ssl.key-alias"));
        node.get(type, Constants.KEY_ALIAS, REQUIRED).set(false);

        node.get(type, Constants.PASSWORD, TYPE).set(ModelType.STRING);
        node.get(type, Constants.PASSWORD, DESCRIPTION).set(bundle.getString("web.connector.ssl.password"));
        node.get(type, Constants.PASSWORD, REQUIRED).set(false);
        node.get(type, Constants.PASSWORD, ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(true);

        node.get(type, Constants.CERTIFICATE_KEY_FILE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.CERTIFICATE_KEY_FILE, DESCRIPTION).set(bundle.getString("web.connector.ssl.certificate-key-file"));
        node.get(type, Constants.CERTIFICATE_KEY_FILE, REQUIRED).set(false);
        node.get(type, Constants.CERTIFICATE_KEY_FILE, ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(true);

        node.get(type, Constants.CIPHER_SUITE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.CIPHER_SUITE, DESCRIPTION).set(bundle.getString("web.connector.ssl.cipher-suite"));
        node.get(type, Constants.CIPHER_SUITE, REQUIRED).set(false);

        node.get(type, Constants.PROTOCOL, TYPE).set(ModelType.STRING);
        node.get(type, Constants.PROTOCOL, DESCRIPTION).set(bundle.getString("web.connector.ssl.protocol"));
        node.get(type, Constants.PROTOCOL, REQUIRED).set(false);

        node.get(type, Constants.VERIFY_CLIENT, TYPE).set(ModelType.STRING);
        node.get(type, Constants.VERIFY_CLIENT, DESCRIPTION).set(bundle.getString("web.connector.ssl.verify-client"));
        node.get(type, Constants.VERIFY_CLIENT, REQUIRED).set(false);

        node.get(type, Constants.VERIFY_DEPTH, TYPE).set(ModelType.INT);
        node.get(type, Constants.VERIFY_DEPTH, DESCRIPTION).set(bundle.getString("web.connector.ssl.verify-depth"));
        node.get(type, Constants.VERIFY_DEPTH, REQUIRED).set(false);

        node.get(type, Constants.CERTIFICATE_FILE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.CERTIFICATE_FILE, DESCRIPTION).set(bundle.getString("web.connector.ssl.certificate-file"));
        node.get(type, Constants.CERTIFICATE_FILE, REQUIRED).set(false);
        node.get(type, Constants.CERTIFICATE_FILE, ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(true);

        node.get(type, Constants.CA_CERTIFICATE_FILE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.CA_CERTIFICATE_FILE, DESCRIPTION).set(bundle.getString("web.connector.ssl.ca-certificate-file"));
        node.get(type, Constants.CA_CERTIFICATE_FILE, REQUIRED).set(false);
        node.get(type, Constants.CA_CERTIFICATE_FILE, ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(true);

        node.get(type, Constants.CA_CERTIFICATE_PASSWORD, TYPE).set(ModelType.STRING);
        node.get(type, Constants.CA_CERTIFICATE_PASSWORD, DESCRIPTION).set(bundle.getString("web.connector.ssl.ca-certificate-file"));
        node.get(type, Constants.CA_CERTIFICATE_PASSWORD, REQUIRED).set(false);
        node.get(type, Constants.CA_CERTIFICATE_PASSWORD, ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(true);

        node.get(type, Constants.CA_REVOCATION_URL, TYPE).set(ModelType.STRING);
        node.get(type, Constants.CA_REVOCATION_URL, DESCRIPTION).set(bundle.getString("web.connector.ssl.ca-revocation-url"));
        node.get(type, Constants.CA_REVOCATION_URL, REQUIRED).set(false);

        node.get(type, Constants.SESSION_CACHE_SIZE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SESSION_CACHE_SIZE, DESCRIPTION).set(bundle.getString("web.connector.ssl.session-cache-size"));
        node.get(type, Constants.SESSION_CACHE_SIZE, REQUIRED).set(false);

        node.get(type, Constants.SESSION_TIMEOUT, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SESSION_TIMEOUT, DESCRIPTION).set(bundle.getString("web.connector.ssl.session-timeout"));
        node.get(type, Constants.SESSION_TIMEOUT, REQUIRED).set(false);

        return node;
    }

    static ModelNode getSSLCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(DESCRIPTION).set(bundle.getString("web.connector.ssl"));
        return addSSLCommonDescription(node, type, bundle);
    }

    static ModelNode getConnectorAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("web.connector.add"));

        getConnectorCommonDescription(node, REQUEST_PROPERTIES, bundle);

        /* add the ssl descriptions */
        ModelNode ssl = node.get(REQUEST_PROPERTIES, Constants.SSL);
        getSSLCommonDescription(ssl, "value-type", bundle);
        ssl.get(TYPE).set(ModelType.OBJECT);
        ssl.get(REQUIRED).set(false);

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

        return getVirtualServerCommonDescription(node, ATTRIBUTES, bundle);
    }

    static ModelNode getVirtualServerCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server"));

        node.get(type, Constants.NAME, TYPE).set(ModelType.STRING);
        node.get(type, Constants.NAME, DESCRIPTION).set(bundle.getString("web.virtual-server.name"));
        node.get(type, Constants.NAME, REQUIRED).set(false); // TODO true
        node.get(type, Constants.NAME, NILLABLE).set(false);

        node.get(type, Constants.ALIAS, TYPE).set(ModelType.LIST);
        node.get(type, Constants.ALIAS, VALUE_TYPE).set(ModelType.STRING);
        node.get(type, Constants.ALIAS, DESCRIPTION).set(bundle.getString("web.virtual-server.alias"));
        node.get(type, Constants.ALIAS, REQUIRED).set(false);
        node.get(type, Constants.ALIAS, NILLABLE).set(true);

        node.get(type, Constants.DEFAULT_WEB_MODULE, TYPE).set(ModelType.STRING);
        node.get(type, Constants.DEFAULT_WEB_MODULE, DESCRIPTION).set(bundle.getString("web.virtual-server.default-web-module"));
        node.get(type, Constants.DEFAULT_WEB_MODULE, REQUIRED).set(false);
        node.get(type, Constants.DEFAULT_WEB_MODULE, DEFAULT).set("ROOT.war");

        node.get(type, Constants.ENABLE_WELCOME_ROOT, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.ENABLE_WELCOME_ROOT, DESCRIPTION).set(bundle.getString("web.virtual-server.enable-welcome-root"));
        node.get(type, Constants.ENABLE_WELCOME_ROOT, REQUIRED).set(false);

        if (ATTRIBUTES.equals(type)) {
            node.get(CHILDREN, Constants.ACCESS_LOG, DESCRIPTION).set(bundle.getString("web.virtual-server.access-log"));
            node.get(CHILDREN, Constants.ACCESS_LOG, MODEL_DESCRIPTION).setEmptyObject();
            node.get(CHILDREN, Constants.REWRITE, DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite"));
            node.get(CHILDREN, Constants.REWRITE, MODEL_DESCRIPTION).setEmptyObject();
            node.get(CHILDREN, Constants.SSO, DESCRIPTION).set(bundle.getString("web.virtual-server.sso"));
            node.get(CHILDREN, Constants.SSO, MODEL_DESCRIPTION).setEmptyObject();
        }

        return node;
    }

    static ModelNode getSsoCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.sso"));

        addSsoCommonDescription(node, type, bundle);

        return node;
    }
    static void addSsoCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {
        node.get(type, Constants.CACHE_CONTAINER, TYPE).set(ModelType.STRING);
        node.get(type, Constants.CACHE_CONTAINER, DESCRIPTION).set(bundle.getString("web.virtual-server.sso.cache-container"));
        node.get(type, Constants.CACHE_CONTAINER, REQUIRED).set(false);

        node.get(type, Constants.CACHE_NAME, TYPE).set(ModelType.STRING);
        node.get(type, Constants.CACHE_NAME, DESCRIPTION).set(bundle.getString("web.virtual-server.sso.cache-name"));
        node.get(type, Constants.CACHE_NAME, REQUIRED).set(false);


        node.get(type, Constants.DOMAIN, TYPE).set(ModelType.STRING);
        node.get(type, Constants.DOMAIN, DESCRIPTION).set(bundle.getString("web.virtual-server.sso.domain"));
        node.get(type, Constants.DOMAIN, REQUIRED).set(false);

        node.get(type, Constants.REAUTHENTICATE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.REAUTHENTICATE, DESCRIPTION).set(bundle.getString("web.virtual-server.sso.reauthenticate"));
        node.get(type, Constants.REAUTHENTICATE, REQUIRED).set(false);
        node.get(type, Constants.REAUTHENTICATE, DEFAULT).set(false);
    }

    static ModelNode getAccessLogCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.access-log"));

        addAccessLogCommonDescription(node, type, bundle);

        return node;
    }

    static void addAccessLogCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(type, Constants.PATTERN, TYPE).set(ModelType.STRING);
        node.get(type, Constants.PATTERN, DESCRIPTION).set(bundle.getString("web.virtual-server.access-log.pattern"));
        node.get(type, Constants.PATTERN, REQUIRED).set(false);
        node.get(type, Constants.PATTERN, DEFAULT).set("common");

        node.get(type, Constants.RESOLVE_HOSTS, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.RESOLVE_HOSTS, DESCRIPTION).set(bundle.getString("web.virtual-server.access-log.resolve-hosts"));
        node.get(type, Constants.RESOLVE_HOSTS, REQUIRED).set(false);
        node.get(type, Constants.RESOLVE_HOSTS, DEFAULT).set(false);

        node.get(type, Constants.EXTENDED, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.EXTENDED, DESCRIPTION).set(bundle.getString("web.virtual-server.access-log.extended"));
        node.get(type, Constants.EXTENDED, REQUIRED).set(false);
        node.get(type, Constants.EXTENDED, DEFAULT).set(false);

        node.get(type, Constants.PREFIX, TYPE).set(ModelType.STRING);
        node.get(type, Constants.PREFIX, DESCRIPTION).set(bundle.getString("web.virtual-server.access-log.prefix"));
        node.get(type, Constants.PREFIX, REQUIRED).set(false);

        node.get(type, Constants.ROTATE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, Constants.ROTATE, DESCRIPTION).set(bundle.getString("web.virtual-server.access-log.rotate"));
        node.get(type, Constants.ROTATE, REQUIRED).set(false);
        node.get(type, Constants.ROTATE, DEFAULT).set(true);

        if (ATTRIBUTES.equals(type)) {
            node.get(CHILDREN, Constants.DIRECTORY, DESCRIPTION).set(bundle.getString("web.virtual-server.access-log.directory"));
            node.get(CHILDREN, Constants.DIRECTORY, MODEL_DESCRIPTION);
        }

    }


    static ModelNode getRewriteCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite"));

        addRewriteCommonDescription(node, type, bundle);

        if (ATTRIBUTES.equals(type)) {
            node.get(CHILDREN, Constants.CONDITION, DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite.condition"));
            node.get(CHILDREN, Constants.CONDITION, MODEL_DESCRIPTION);
        }

        return node;
    }

    static void addRewriteCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {
        node.get(type, Constants.PATTERN, TYPE).set(ModelType.STRING);
        node.get(type, Constants.PATTERN, DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite.pattern"));
        node.get(type, Constants.PATTERN, REQUIRED).set(false);

        node.get(type, Constants.SUBSTITUTION, TYPE).set(ModelType.STRING);
        node.get(type, Constants.SUBSTITUTION, DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite.substitution"));
        node.get(type, Constants.SUBSTITUTION, REQUIRED).set(false);

        node.get(type, Constants.FLAGS, TYPE).set(ModelType.STRING);
        node.get(type, Constants.FLAGS, DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite.flags"));
        node.get(type, Constants.FLAGS, REQUIRED).set(false);
    }
    static void addRewriteConditionnCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(type, Constants.TEST, TYPE).set(ModelType.STRING);
        node.get(type, Constants.TEST, DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite.condition.test"));
        node.get(type, Constants.TEST, REQUIRED).set(false);

        node.get(type, Constants.PATTERN, TYPE).set(ModelType.STRING);
        node.get(type, Constants.PATTERN, DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite.condition.pattern"));
        node.get(type, Constants.PATTERN, REQUIRED).set(false);

        node.get(type, Constants.FLAGS, TYPE).set(ModelType.STRING);
        node.get(type, Constants.FLAGS, DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite.condition.flags"));
        node.get(type, Constants.FLAGS, REQUIRED).set(false);
    }

    static ModelNode getVirtualServerAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.add"));

        getVirtualServerCommonDescription(node, REQUEST_PROPERTIES, bundle);

        ModelNode accesslog = node.get(REQUEST_PROPERTIES, Constants.ACCESS_LOG);
        getAccessLogCommonDescription(accesslog, "value-type", bundle);
        accesslog.get(REQUIRED).set(false);
        accesslog.get(TYPE).set(ModelType.OBJECT);
        ModelNode rewrite = node.get(REQUEST_PROPERTIES, Constants.REWRITE);
        getRewriteCommonDescription(rewrite, "value-type", bundle);
        rewrite.get(REQUIRED).set(false);
        rewrite.get(TYPE).set(ModelType.OBJECT);
        ModelNode sso = node.get(REQUEST_PROPERTIES, Constants.SSO);
        getSsoCommonDescription(sso, "value-type", bundle);
        sso.get(REQUIRED).set(false);
        sso.get(TYPE).set(ModelType.OBJECT);

        return node;
    }

    static ModelNode getVirtualServerRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.remove"));

        return node;
    }

    static ModelNode getDeploymentRuntimeDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();

        node.get(DESCRIPTION).set(bundle.getString("web.deployment"));
        node.get(ATTRIBUTES).setEmptyObject();
        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN, "servlet", DESCRIPTION).set(bundle.getString("web.deployment.servlet"));
        node.get(CHILDREN, "servlet", MIN_OCCURS).set(0);
        node.get(CHILDREN, "servlet", MODEL_DESCRIPTION);

        return node;
    }

    static ModelNode getDeploymentServletDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();

        node.get(DESCRIPTION).set(bundle.getString("web.deployment.servlet"));

        node.get(ATTRIBUTES, "load-time", DESCRIPTION).set(bundle.getString("web.deployment.servlet.load-time"));
        node.get(ATTRIBUTES, "load-time", TYPE).set(ModelType.LONG);
        node.get(ATTRIBUTES, "max-time", DESCRIPTION).set(bundle.getString("web.deployment.servlet.max-time"));
        node.get(ATTRIBUTES, "max-time", TYPE).set(ModelType.LONG);
        node.get(ATTRIBUTES, "min-time", DESCRIPTION).set(bundle.getString("web.deployment.servlet.min-time"));
        node.get(ATTRIBUTES, "min-time", TYPE).set(ModelType.LONG);
        node.get(ATTRIBUTES, "processing-time", DESCRIPTION).set(bundle.getString("web.deployment.servlet.processing-time"));
        node.get(ATTRIBUTES, "processing-time", TYPE).set(ModelType.LONG);
        node.get(ATTRIBUTES, "request-count", DESCRIPTION).set(bundle.getString("web.deployment.servlet.request-count"));
        node.get(ATTRIBUTES, "request-count", TYPE).set(ModelType.INT);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }


    public static ModelNode getJspConfigurationDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("web.configuration.jsp"));

        return getJSPCommonDescription(node, ATTRIBUTES, bundle);
    }
    public static ModelNode getJspConfigurationDescription(final ModelNode node, final String type, final ResourceBundle bundle) {
        node.get(DESCRIPTION).set(bundle.getString("web.configuration.jsp"));

        return getJSPCommonDescription(node, type, bundle);
    }

    public static ModelNode getStaticResourceDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("web.configuration.static"));

        return getStaticResourcesCommonDescription(node, ATTRIBUTES, bundle);
    }
    public static ModelNode getStaticResourceDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(DESCRIPTION).set(bundle.getString("web.configuration.static"));
        return getStaticResourcesCommonDescription(node, type, bundle);
    }


    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static ModelNode getConfigurationDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("web.configuration"));

        node.get(CHILDREN, Constants.STATIC_RESOURCES, DESCRIPTION).set(bundle.getString("web.configuration.static"));
        node.get(CHILDREN, Constants.STATIC_RESOURCES, MODEL_DESCRIPTION).setEmptyObject();
        node.get(CHILDREN, Constants.JSP_CONFIGURATION, DESCRIPTION).set(bundle.getString("web.configuration.jsp"));
        node.get(CHILDREN, Constants.JSP_CONFIGURATION, MODEL_DESCRIPTION).setEmptyObject();
        node.get(CHILDREN, Constants.CONTAINER, DESCRIPTION).set(bundle.getString("web.configuration.container"));
        node.get(CHILDREN, Constants.CONTAINER, MODEL_DESCRIPTION).setEmptyObject();

        return node;
    }
    public static ModelNode getContainerDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("web.configuration.container"));
        return node;
    }

    public static ModelNode getMimeMappingAddDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("add-mime");
        node.get(DESCRIPTION).set(bundle.getString("web.configuration.add-mime"));

        node.get(REQUEST_PROPERTIES, "name", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "name", DESCRIPTION).set(bundle.getString("web.configuration.mime-mapping.name"));
        node.get(REQUEST_PROPERTIES, "name", REQUIRED).set(true);

        node.get(REQUEST_PROPERTIES, "value", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "value", DESCRIPTION).set(bundle.getString("web.configuration.mime-mapping.value"));
        node.get(REQUEST_PROPERTIES, "value", REQUIRED).set(true);

        return node;
    }

    public static ModelNode getMimeMappingRemoveDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("remove-mime");
        node.get(DESCRIPTION).set(bundle.getString("web.configuration.remove-mime"));

        node.get(REQUEST_PROPERTIES, "name", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "name", DESCRIPTION).set(bundle.getString("web.configuration.mime-mapping.name"));
        node.get(REQUEST_PROPERTIES, "name", REQUIRED).set(true);

        return node;
    }

    public static ModelNode getReWriteDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite"));
        return getRewriteCommonDescription(node, ATTRIBUTES, bundle);
    }

    public static ModelNode getAccessLogDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.access-log"));
        return getAccessLogCommonDescription(node, ATTRIBUTES, bundle);
    }

    public static ModelNode getDirectoryDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.access-log.directory"));
        return node;
    }

    public static ModelNode getReWriteCondDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite.condition"));
        return node;
    }

    public static ModelNode getAccessLogAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.access-log-add"));

        addAccessLogCommonDescription(node, REQUEST_PROPERTIES, bundle);
        return node;
    }

    public static ModelNode getAccessLogRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.access-log-remove"));
        return node;
    }

    public static ModelNode getReWriteRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite-remove"));
        return node;
    }

    public static ModelNode getReWriteAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite-add"));

        addRewriteCommonDescription(node, REQUEST_PROPERTIES, bundle);

        return node;
    }

    public static ModelNode getReWriteConditionAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite.condition-add"));

        addRewriteConditionnCommonDescription(node, REQUEST_PROPERTIES, bundle);

        return node;
    }

    public static ModelNode getReWriteConditionRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.rewrite.condition-remove"));
        return node;
    }

    public static ModelNode getSSLDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        return getSSLCommonDescription(node, ATTRIBUTES, bundle);
    }

    public static ModelNode getSSODescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.sso"));
        return node;
    }

    public static ModelNode getSSOAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.sso-add"));

        addSsoCommonDescription(node, REQUEST_PROPERTIES, bundle);
        return node;
    }

    public static ModelNode getSSORemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("web.virtual-server.sso-remove"));
        return node;
    }
    public static ModelNode getSSLAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("web.connector.ssl-add"));

        addSSLCommonDescription(node, REQUEST_PROPERTIES, bundle);
        return node;
    }

    public static ModelNode getSSLRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("web.connector.ssl-remove"));
        return node;
    }

}
