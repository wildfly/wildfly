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

package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * The modcluster subsystem description providers.
 *
 * @author Jean-Frederic Clere
 */
class ModClusterSubsystemDescriptions {

    static final String RESOURCE_NAME = ModClusterSubsystemDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    static ModelNode getSubsystemDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();

        node.get(DESCRIPTION).set(bundle.getString("modcluster"));
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);
        node.get(NAMESPACE).set(Namespace.MODCLUSTER.getUriString());
        getConfigurationCommonDescription(node.get(ATTRIBUTES, CommonAttributes.MOD_CLUSTER_CONFIG), ATTRIBUTES, bundle);

        return node;
    }

    static ModelNode getSubsystemAddDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("modcluster.add"));
        return node;
    }

    static ModelNode getListProxiesDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("list-proxies");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.list-proxies"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("modcluster.proxy-list"));
        node.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        return node;
    }

    static void AddHostPortDescription(ModelNode node, ResourceBundle bundle) {
        node.get(REQUEST_PROPERTIES, "host", DESCRIPTION).set(bundle.getString("modcluster.proxy-host"));
        node.get(REQUEST_PROPERTIES, "host", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "host", REQUIRED).set(true);

        node.get(REQUEST_PROPERTIES, "port",DESCRIPTION).set(bundle.getString("modcluster.proxy-port"));
        node.get(REQUEST_PROPERTIES, "port", TYPE).set(ModelType.INT);
        node.get(REQUEST_PROPERTIES, "port", REQUIRED).set(true);
    }
    static ModelNode getAddProxyDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("add-proxy");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.add-proxy"));

        AddHostPortDescription(node, bundle);

        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getRemoveProxyDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("remove-proxy");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.remove-proxy"));

        AddHostPortDescription(node, bundle);

        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getRefreshDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("refresh");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.refresh"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getResetDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("reset");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.reset"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }
    static ModelNode getEnableDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("enable");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.enable"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static void AddWaitTimeDescription(ModelNode node, ResourceBundle bundle) {
        node.get(REQUEST_PROPERTIES, "waittime",DESCRIPTION).set(bundle.getString("modcluster.waittime"));
        node.get(REQUEST_PROPERTIES, "waittime", TYPE).set(ModelType.INT);
        node.get(REQUEST_PROPERTIES, "waittime", REQUIRED).set(false);
    }
    static ModelNode getStopDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("stop");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.stop"));
        AddWaitTimeDescription(node, bundle);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getDisableDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("disable");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.disable"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static void AddHostContextDescription(ModelNode node, ResourceBundle bundle) {
        node.get(REQUEST_PROPERTIES, "virtualhost", DESCRIPTION).set(bundle.getString("modcluster.virtualhost"));
        node.get(REQUEST_PROPERTIES, "virtualhost", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "virtualhost", REQUIRED).set(true);

        node.get(REQUEST_PROPERTIES, "context",DESCRIPTION).set(bundle.getString("modcluster.context"));
        node.get(REQUEST_PROPERTIES, "context", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "context", REQUIRED).set(true);
    }

    static ModelNode getEnableContextDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("enable-context");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.enable-context"));
        AddHostContextDescription(node, bundle);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getDisableContextDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("disable-context");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.disable-context"));
        AddHostContextDescription(node, bundle);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getStopContextDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("stop-context");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.stop-context"));
        AddHostContextDescription(node, bundle);
        AddWaitTimeDescription(node, bundle);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    static ModelNode getConfigurationCommonDescription(final ModelNode node, final String type, final ResourceBundle bundle) {

        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("modcluster.configuration"));
        node.get(REQUIRED).set(false);

        node.get(type, CommonAttributes.ADVERTISE_SOCKET, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.ADVERTISE_SOCKET, DESCRIPTION).set(bundle.getString("modcluster.configuration.advertise-socket"));
        node.get(type, CommonAttributes.ADVERTISE_SOCKET, REQUIRED).set(false);
        node.get(type, CommonAttributes.ADVERTISE_SOCKET, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.PROXY_LIST, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.PROXY_LIST, DESCRIPTION).set(bundle.getString("modcluster.configuration.proxy-list"));
        node.get(type, CommonAttributes.PROXY_LIST, REQUIRED).set(false);
        node.get(type, CommonAttributes.PROXY_LIST, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.PROXY_URL, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.PROXY_URL, DESCRIPTION).set(bundle.getString("modcluster.configuration.proxy-url"));
        node.get(type, CommonAttributes.PROXY_URL, REQUIRED).set(false);
        node.get(type, CommonAttributes.PROXY_URL, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.ADVERTISE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.ADVERTISE, DESCRIPTION).set(bundle.getString("modcluster.configuration.advertise"));
        node.get(type, CommonAttributes.ADVERTISE, REQUIRED).set(false);
        node.get(type, CommonAttributes.ADVERTISE, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.ADVERTISE_SECURITY_KEY, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.ADVERTISE_SECURITY_KEY, DESCRIPTION).set(bundle.getString("modcluster.configuration.advertise-security-key"));
        node.get(type, CommonAttributes.ADVERTISE_SECURITY_KEY, REQUIRED).set(false);
        node.get(type, CommonAttributes.ADVERTISE_SECURITY_KEY, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.EXCLUDED_CONTEXTS, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.EXCLUDED_CONTEXTS, DESCRIPTION).set(bundle.getString("modcluster.configuration.excluded-contexts"));
        node.get(type, CommonAttributes.EXCLUDED_CONTEXTS, REQUIRED).set(false);
        node.get(type, CommonAttributes.EXCLUDED_CONTEXTS, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.AUTO_ENABLE_CONTEXTS, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.AUTO_ENABLE_CONTEXTS, DESCRIPTION).set(bundle.getString("modcluster.configuration.auto-enable-contexts"));
        node.get(type, CommonAttributes.AUTO_ENABLE_CONTEXTS, REQUIRED).set(false);
        node.get(type, CommonAttributes.AUTO_ENABLE_CONTEXTS, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.STOP_CONTEXT_TIMEOUT, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.STOP_CONTEXT_TIMEOUT, DESCRIPTION).set(bundle.getString("modcluster.configuration.stop-context-timeout"));
        node.get(type, CommonAttributes.STOP_CONTEXT_TIMEOUT, REQUIRED).set(false);
        node.get(type, CommonAttributes.STOP_CONTEXT_TIMEOUT, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.SOCKET_TIMEOUT, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.SOCKET_TIMEOUT, DESCRIPTION).set(bundle.getString("modcluster.configuration.socket-timeout"));
        node.get(type, CommonAttributes.SOCKET_TIMEOUT, REQUIRED).set(false);
        node.get(type, CommonAttributes.SOCKET_TIMEOUT, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.STICKY_SESSION, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.STICKY_SESSION, DESCRIPTION).set(bundle.getString("modcluster.configuration.sticky-session"));
        node.get(type, CommonAttributes.STICKY_SESSION, REQUIRED).set(false);
        node.get(type, CommonAttributes.STICKY_SESSION, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.STICKY_SESSION_REMOVE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.STICKY_SESSION_REMOVE, DESCRIPTION).set(bundle.getString("modcluster.configuration.sticky-session-remove"));
        node.get(type, CommonAttributes.STICKY_SESSION_REMOVE, REQUIRED).set(false);
        node.get(type, CommonAttributes.STICKY_SESSION_REMOVE, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.STICKY_SESSION_FORCE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.STICKY_SESSION_FORCE, DESCRIPTION).set(bundle.getString("modcluster.configuration.sticky-session-force"));
        node.get(type, CommonAttributes.STICKY_SESSION_FORCE, REQUIRED).set(false);
        node.get(type, CommonAttributes.STICKY_SESSION_FORCE, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.WORKER_TIMEOUT, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.WORKER_TIMEOUT, DESCRIPTION).set(bundle.getString("modcluster.configuration.worker-timeout"));
        node.get(type, CommonAttributes.WORKER_TIMEOUT, REQUIRED).set(false);
        node.get(type, CommonAttributes.WORKER_TIMEOUT, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.MAX_ATTEMPTS, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.MAX_ATTEMPTS, DESCRIPTION).set(bundle.getString("modcluster.configuration.max-attemps"));
        node.get(type, CommonAttributes.MAX_ATTEMPTS, REQUIRED).set(false);
        node.get(type, CommonAttributes.MAX_ATTEMPTS, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.FLUSH_PACKETS, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.FLUSH_PACKETS, DESCRIPTION).set(bundle.getString("modcluster.configuration.flush-packets"));
        node.get(type, CommonAttributes.FLUSH_PACKETS, REQUIRED).set(false);
        node.get(type, CommonAttributes.FLUSH_PACKETS, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.FLUSH_WAIT, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.FLUSH_WAIT, DESCRIPTION).set(bundle.getString("modcluster.configuration.flush-wait"));
        node.get(type, CommonAttributes.FLUSH_WAIT, REQUIRED).set(false);
        node.get(type, CommonAttributes.FLUSH_WAIT, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.PING, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.PING, DESCRIPTION).set(bundle.getString("modcluster.configuration.ping"));
        node.get(type, CommonAttributes.PING, REQUIRED).set(false);
        node.get(type, CommonAttributes.PING, MAX_OCCURS).set(1);


        node.get(type, CommonAttributes.SMAX, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.SMAX, DESCRIPTION).set(bundle.getString("modcluster.configuration.smax"));
        node.get(type, CommonAttributes.SMAX, REQUIRED).set(false);
        node.get(type, CommonAttributes.SMAX, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.TTL, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.TTL, DESCRIPTION).set(bundle.getString("modcluster.configuration.ttl"));
        node.get(type, CommonAttributes.TTL, REQUIRED).set(false);
        node.get(type, CommonAttributes.TTL, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.NODE_TIMEOUT, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.NODE_TIMEOUT, DESCRIPTION).set(bundle.getString("modcluster.configuration.node-timeout"));
        node.get(type, CommonAttributes.NODE_TIMEOUT, REQUIRED).set(false);
        node.get(type, CommonAttributes.NODE_TIMEOUT, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.BALANCER, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.BALANCER, DESCRIPTION).set(bundle.getString("modcluster.configuration.balancer"));
        node.get(type, CommonAttributes.BALANCER, REQUIRED).set(false);
        node.get(type, CommonAttributes.BALANCER, MAX_OCCURS).set(1);

        node.get(type, CommonAttributes.DOMAIN, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.DOMAIN, DESCRIPTION).set(bundle.getString("modcluster.configuration.domain"));
        node.get(type, CommonAttributes.DOMAIN, REQUIRED).set(false);
        node.get(type, CommonAttributes.DOMAIN, MAX_OCCURS).set(1);

        getSSLCommonDescription(node.get(CHILDREN, CommonAttributes.SSL), ATTRIBUTES, bundle);
        return node;
    }

    private static void getSSLCommonDescription(ModelNode node, String type, ResourceBundle bundle) {
        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("web.connector.ssl"));
        node.get(REQUIRED).set(false);

        node.get(type, CommonAttributes.KEY_ALIAS, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.KEY_ALIAS, DESCRIPTION).set(bundle.getString("web.connector.ssl.key-alias"));
        node.get(type, CommonAttributes.KEY_ALIAS, REQUIRED).set(false);

        node.get(type, CommonAttributes.PASSWORD, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.PASSWORD, DESCRIPTION).set(bundle.getString("web.connector.ssl.password"));
        node.get(type, CommonAttributes.PASSWORD, REQUIRED).set(false);

        node.get(type, CommonAttributes.CERTIFICATE_KEY_FILE, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.CERTIFICATE_KEY_FILE, DESCRIPTION).set(bundle.getString("web.connector.ssl.certificate-key-file"));
        node.get(type, CommonAttributes.CERTIFICATE_KEY_FILE, REQUIRED).set(false);

        node.get(type, CommonAttributes.CIPHER_SUITE, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.CIPHER_SUITE, DESCRIPTION).set(bundle.getString("web.connector.ssl.cipher-suite"));
        node.get(type, CommonAttributes.CIPHER_SUITE, REQUIRED).set(false);

        node.get(type, CommonAttributes.PROTOCOL, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.PROTOCOL, DESCRIPTION).set(bundle.getString("web.connector.ssl.protocol"));
        node.get(type, CommonAttributes.PROTOCOL, REQUIRED).set(false);

        node.get(type, CommonAttributes.CA_CERTIFICATE_FILE, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.CA_CERTIFICATE_FILE, DESCRIPTION).set(bundle.getString("web.connector.ssl.ca-certificate-file"));
        node.get(type, CommonAttributes.CA_CERTIFICATE_FILE, REQUIRED).set(false);

        node.get(type, CommonAttributes.CA_REVOCATION_URL, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.CA_REVOCATION_URL, DESCRIPTION).set(bundle.getString("web.connector.ssl.ca-revocation-url"));
        node.get(type, CommonAttributes.CA_REVOCATION_URL, REQUIRED).set(false);

    }
}
