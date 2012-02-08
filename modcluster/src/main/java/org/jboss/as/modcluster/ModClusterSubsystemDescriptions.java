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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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
        node.get(NAMESPACE).set(Namespace.CURRENT.getUri());

        node.get(CHILDREN, CommonAttributes.MOD_CLUSTER_CONFIG, DESCRIPTION).set(bundle.getString("modcluster.configuration"));
        node.get(CHILDREN, CommonAttributes.MOD_CLUSTER_CONFIG, MODEL_DESCRIPTION).setEmptyObject();

        return node;
    }

    static ModelNode getSubsystemAddDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("modcluster.add"));

        ModelNode configuration = node.get(REQUEST_PROPERTIES, CommonAttributes.MOD_CLUSTER_CONFIG);
        getConfigurationCommonDescription(configuration, "value-type", bundle);
        configuration.get(TYPE).set(ModelType.OBJECT);
        configuration.get(REQUIRED).set(false);

        return node;
    }

    static ModelNode getSubsystemRemoveDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("modcluster.remove"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();

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

    static ModelNode getProxyInfoDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("read-proxies-info");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.read-proxies-info"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("modcluster.proxies-info"));
        node.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        return node;
    }

    static ModelNode getProxyConfigurationDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("read-proxies-configuration");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.read-proxies-configuration"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("modcluster.proxies-configuration"));
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

    /* Something like:
     * <load-metric type="cpu" weight="2" capacity="1" (properties) .../>
     * or
     * <custom-load-metric class="classname" weight="2" capacity="1" (properties) ... />
     */
    static void addCommonMetricDescription(ModelNode node, ResourceBundle bundle) {

        node.get(REQUEST_PROPERTIES, "weight",DESCRIPTION).set(bundle.getString("modcluster.configuration.metric.weight"));
        node.get(REQUEST_PROPERTIES, "weight", TYPE).set(ModelType.INT);
        node.get(REQUEST_PROPERTIES, "weight", REQUIRED).set(false);

        node.get(REQUEST_PROPERTIES, "capacity",DESCRIPTION).set(bundle.getString("modcluster.configuration.metric.capacity"));
        node.get(REQUEST_PROPERTIES, "capacity", TYPE).set(ModelType.INT);
        node.get(REQUEST_PROPERTIES, "capacity", REQUIRED).set(false);

        node.get(REQUEST_PROPERTIES, "property",DESCRIPTION).set(bundle.getString("modcluster.configuration.metric.property"));
        node.get(REQUEST_PROPERTIES, "property", TYPE).set(ModelType.PROPERTY);
        node.get(REQUEST_PROPERTIES, "property", REQUIRED).set(false);
    }
    static ModelNode getAddMetricDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("add-metric");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.configuration.add-metric"));

        node.get(REQUEST_PROPERTIES, "type", DESCRIPTION).set(bundle.getString("modcluster.configuration.metric.type"));
        node.get(REQUEST_PROPERTIES, "type", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "type", REQUIRED).set(true);

        addCommonMetricDescription(node, bundle);

        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;

    }
    static ModelNode getRemoveMetricDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("remove-metric");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.configuration.add-metric"));

        node.get(REQUEST_PROPERTIES, "type", DESCRIPTION).set(bundle.getString("modcluster.configuration.metric.type"));
        node.get(REQUEST_PROPERTIES, "type", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "type", REQUIRED).set(true);

        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;

    }
    static ModelNode getAddCustomMetricDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("add-custom-metric");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.configuration.add-custom-metric"));

        node.get(REQUEST_PROPERTIES, "class", DESCRIPTION).set(bundle.getString("modcluster.configuration.metric.class"));
        node.get(REQUEST_PROPERTIES, "class", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "class", REQUIRED).set(true);

        addCommonMetricDescription(node, bundle);

        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }
    static ModelNode getRemoveCustomMetricDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("remove-custom-metric");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.configuration.remove-custom-metric"));

        node.get(REQUEST_PROPERTIES, "class", DESCRIPTION).set(bundle.getString("modcluster.configuration.metric.class"));
        node.get(REQUEST_PROPERTIES, "class", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "class", REQUIRED).set(true);

        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
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

    static void addHostContextDescription(ModelNode node, ResourceBundle bundle) {
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
        addHostContextDescription(node, bundle);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getDisableContextDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("disable-context");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.disable-context"));
        addHostContextDescription(node, bundle);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getStopContextDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("stop-context");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.stop-context"));
        addHostContextDescription(node, bundle);
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

        node.get(DESCRIPTION).set(bundle.getString("modcluster.configuration"));

        node.get(type, CommonAttributes.ADVERTISE_SOCKET, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.ADVERTISE_SOCKET, DESCRIPTION).set(bundle.getString("modcluster.configuration.advertise-socket"));
        node.get(type, CommonAttributes.ADVERTISE_SOCKET, REQUIRED).set(false);
        node.get(type, CommonAttributes.ADVERTISE_SOCKET,  DEFAULT).set("224.0.1.105:23364");

        node.get(type, CommonAttributes.PROXY_LIST, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.PROXY_LIST, DESCRIPTION).set(bundle.getString("modcluster.configuration.proxy-list"));
        node.get(type, CommonAttributes.PROXY_LIST, REQUIRED).set(false);
        node.get(type, CommonAttributes.PROXY_LIST, EXPRESSIONS_ALLOWED).set(true);
        node.get(type, CommonAttributes.PROXY_LIST, DEFAULT).set("");

        node.get(type, CommonAttributes.PROXY_URL, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.PROXY_URL, DESCRIPTION).set(bundle.getString("modcluster.configuration.proxy-url"));
        node.get(type, CommonAttributes.PROXY_URL, REQUIRED).set(false);
        node.get(type, CommonAttributes.PROXY_URL, EXPRESSIONS_ALLOWED).set(true);
        node.get(type, CommonAttributes.PROXY_URL, DEFAULT).set("/");

        node.get(type, CommonAttributes.ADVERTISE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.ADVERTISE, DESCRIPTION).set(bundle.getString("modcluster.configuration.advertise"));
        node.get(type, CommonAttributes.ADVERTISE, REQUIRED).set(false);
        node.get(type, CommonAttributes.ADVERTISE, DEFAULT).set(true);

        node.get(type, CommonAttributes.ADVERTISE_SECURITY_KEY, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.ADVERTISE_SECURITY_KEY, DESCRIPTION).set(bundle.getString("modcluster.configuration.advertise-security-key"));
        node.get(type, CommonAttributes.ADVERTISE_SECURITY_KEY, REQUIRED).set(false);
        node.get(type, CommonAttributes.ADVERTISE_SECURITY_KEY, EXPRESSIONS_ALLOWED).set(true);

        node.get(type, CommonAttributes.EXCLUDED_CONTEXTS, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.EXCLUDED_CONTEXTS, DESCRIPTION).set(bundle.getString("modcluster.configuration.excluded-contexts"));
        node.get(type, CommonAttributes.EXCLUDED_CONTEXTS, REQUIRED).set(false);
        node.get(type, CommonAttributes.EXCLUDED_CONTEXTS, EXPRESSIONS_ALLOWED).set(true);
        node.get(type, CommonAttributes.EXCLUDED_CONTEXTS, DEFAULT).set("ROOT,console");

        node.get(type, CommonAttributes.AUTO_ENABLE_CONTEXTS, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.AUTO_ENABLE_CONTEXTS, DESCRIPTION).set(bundle.getString("modcluster.configuration.auto-enable-contexts"));
        node.get(type, CommonAttributes.AUTO_ENABLE_CONTEXTS, REQUIRED).set(false);
        node.get(type, CommonAttributes.AUTO_ENABLE_CONTEXTS, DEFAULT).set(true);

        node.get(type, CommonAttributes.STOP_CONTEXT_TIMEOUT, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.STOP_CONTEXT_TIMEOUT, DESCRIPTION).set(bundle.getString("modcluster.configuration.stop-context-timeout"));
        node.get(type, CommonAttributes.STOP_CONTEXT_TIMEOUT, REQUIRED).set(false);
        node.get(type, CommonAttributes.STOP_CONTEXT_TIMEOUT, DEFAULT).set(10);
        node.get(type, CommonAttributes.STOP_CONTEXT_TIMEOUT, ModelDescriptionConstants.UNIT).set(MeasurementUnit.SECONDS.getName());

        node.get(type, CommonAttributes.SOCKET_TIMEOUT, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.SOCKET_TIMEOUT, DESCRIPTION).set(bundle.getString("modcluster.configuration.socket-timeout"));
        node.get(type, CommonAttributes.SOCKET_TIMEOUT, REQUIRED).set(false);
        node.get(type, CommonAttributes.SOCKET_TIMEOUT, DEFAULT).set(20);
        node.get(type, CommonAttributes.SOCKET_TIMEOUT, ModelDescriptionConstants.UNIT).set(MeasurementUnit.SECONDS.getName());

        node.get(type, CommonAttributes.STICKY_SESSION, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.STICKY_SESSION, DESCRIPTION).set(bundle.getString("modcluster.configuration.sticky-session"));
        node.get(type, CommonAttributes.STICKY_SESSION, REQUIRED).set(false);
        node.get(type, CommonAttributes.STICKY_SESSION, DEFAULT).set(true);

        node.get(type, CommonAttributes.STICKY_SESSION_REMOVE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.STICKY_SESSION_REMOVE, DESCRIPTION).set(bundle.getString("modcluster.configuration.sticky-session-remove"));
        node.get(type, CommonAttributes.STICKY_SESSION_REMOVE, REQUIRED).set(false);
        node.get(type, CommonAttributes.STICKY_SESSION_REMOVE, DEFAULT).set(false);

        node.get(type, CommonAttributes.STICKY_SESSION_FORCE, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.STICKY_SESSION_FORCE, DESCRIPTION).set(bundle.getString("modcluster.configuration.sticky-session-force"));
        node.get(type, CommonAttributes.STICKY_SESSION_FORCE, REQUIRED).set(false);
        node.get(type, CommonAttributes.STICKY_SESSION_FORCE, DEFAULT).set(false);

        node.get(type, CommonAttributes.WORKER_TIMEOUT, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.WORKER_TIMEOUT, DESCRIPTION).set(bundle.getString("modcluster.configuration.worker-timeout"));
        node.get(type, CommonAttributes.WORKER_TIMEOUT, REQUIRED).set(false);
        node.get(type, CommonAttributes.WORKER_TIMEOUT, DEFAULT).set(-1);
        node.get(type, CommonAttributes.WORKER_TIMEOUT, ModelDescriptionConstants.UNIT).set(MeasurementUnit.SECONDS.getName());

        node.get(type, CommonAttributes.MAX_ATTEMPTS, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.MAX_ATTEMPTS, DESCRIPTION).set(bundle.getString("modcluster.configuration.max-attemps"));
        node.get(type, CommonAttributes.MAX_ATTEMPTS, REQUIRED).set(false);
        node.get(type, CommonAttributes.MAX_ATTEMPTS, DEFAULT).set(1);

        node.get(type, CommonAttributes.FLUSH_PACKETS, TYPE).set(ModelType.BOOLEAN);
        node.get(type, CommonAttributes.FLUSH_PACKETS, DESCRIPTION).set(bundle.getString("modcluster.configuration.flush-packets"));
        node.get(type, CommonAttributes.FLUSH_PACKETS, REQUIRED).set(false);
        node.get(type, CommonAttributes.FLUSH_PACKETS, DEFAULT).set(false);


        node.get(type, CommonAttributes.FLUSH_WAIT, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.FLUSH_WAIT, DESCRIPTION).set(bundle.getString("modcluster.configuration.flush-wait"));
        node.get(type, CommonAttributes.FLUSH_WAIT, REQUIRED).set(false);
        node.get(type, CommonAttributes.FLUSH_WAIT, DEFAULT).set(-1);
        node.get(type, CommonAttributes.FLUSH_WAIT, ModelDescriptionConstants.UNIT).set(MeasurementUnit.MILLISECONDS.getName());

        node.get(type, CommonAttributes.PING, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.PING, DESCRIPTION).set(bundle.getString("modcluster.configuration.ping"));
        node.get(type, CommonAttributes.PING, REQUIRED).set(false);
        node.get(type, CommonAttributes.PING, DEFAULT).set(10);
        node.get(type, CommonAttributes.PING, ModelDescriptionConstants.UNIT).set(MeasurementUnit.SECONDS.getName());


        node.get(type, CommonAttributes.SMAX, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.SMAX, DESCRIPTION).set(bundle.getString("modcluster.configuration.smax"));
        node.get(type, CommonAttributes.SMAX, REQUIRED).set(false);

        node.get(type, CommonAttributes.TTL, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.TTL, DESCRIPTION).set(bundle.getString("modcluster.configuration.ttl"));
        node.get(type, CommonAttributes.TTL, REQUIRED).set(false);
        node.get(type, CommonAttributes.TTL, DEFAULT).set(60);
        node.get(type, CommonAttributes.TTL, ModelDescriptionConstants.UNIT).set(MeasurementUnit.SECONDS.getName());

        node.get(type, CommonAttributes.NODE_TIMEOUT, TYPE).set(ModelType.INT);
        node.get(type, CommonAttributes.NODE_TIMEOUT, DESCRIPTION).set(bundle.getString("modcluster.configuration.node-timeout"));
        node.get(type, CommonAttributes.NODE_TIMEOUT, REQUIRED).set(false);
        node.get(type, CommonAttributes.NODE_TIMEOUT, DEFAULT).set(-1);
        node.get(type, CommonAttributes.NODE_TIMEOUT, ModelDescriptionConstants.UNIT).set(MeasurementUnit.SECONDS.getName());

        node.get(type, CommonAttributes.BALANCER, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.BALANCER, DESCRIPTION).set(bundle.getString("modcluster.configuration.balancer"));
        node.get(type, CommonAttributes.BALANCER, REQUIRED).set(false);
        node.get(type, CommonAttributes.BALANCER, DEFAULT).set("mycluster");

        // That is the loadBalancingGroup :-(
        node.get(type, CommonAttributes.DOMAIN, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.DOMAIN, DESCRIPTION).set(bundle.getString("modcluster.configuration.domain"));
        node.get(type, CommonAttributes.DOMAIN, REQUIRED).set(false);
        node.get(type, CommonAttributes.DOMAIN, EXPRESSIONS_ALLOWED).set(true);

        if (ATTRIBUTES.equals(type)) {
            node.get(CHILDREN, CommonAttributes.SSL, DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl"));
            node.get(CHILDREN, CommonAttributes.SSL, MODEL_DESCRIPTION).setEmptyObject();
        }
        return node;
    }

    private static void getSSLCommonDescription(ModelNode node, String type, ResourceBundle bundle) {
        node.get(DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl"));

        node.get(type, CommonAttributes.KEY_ALIAS, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.KEY_ALIAS, DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl.key-alias"));
        node.get(type, CommonAttributes.KEY_ALIAS, REQUIRED).set(false);

        node.get(type, CommonAttributes.PASSWORD, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.PASSWORD, DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl.password"));
        node.get(type, CommonAttributes.PASSWORD, REQUIRED).set(false);
        node.get(type, CommonAttributes.PASSWORD, DEFAULT).set("changeit");
        node.get(type, CommonAttributes.PASSWORD, EXPRESSIONS_ALLOWED).set(true);

        node.get(type, CommonAttributes.CERTIFICATE_KEY_FILE, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.CERTIFICATE_KEY_FILE, DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl.certificate-key-file"));
        node.get(type, CommonAttributes.CERTIFICATE_KEY_FILE, REQUIRED).set(false);
        node.get(type, CommonAttributes.CERTIFICATE_KEY_FILE, DEFAULT).set("${user.home}/.keystore");
        node.get(type, CommonAttributes.CERTIFICATE_KEY_FILE, EXPRESSIONS_ALLOWED).set(true);

        node.get(type, CommonAttributes.CIPHER_SUITE, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.CIPHER_SUITE, DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl.cipher-suite"));
        node.get(type, CommonAttributes.CIPHER_SUITE, REQUIRED).set(false);

        node.get(type, CommonAttributes.PROTOCOL, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.PROTOCOL, DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl.protocol"));
        node.get(type, CommonAttributes.PROTOCOL, REQUIRED).set(false);
        node.get(type, CommonAttributes.PROTOCOL, DEFAULT).set("TLS");

        node.get(type, CommonAttributes.CA_CERTIFICATE_FILE, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.CA_CERTIFICATE_FILE, DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl.ca-certificate-file"));
        node.get(type, CommonAttributes.CA_CERTIFICATE_FILE, REQUIRED).set(false);
        node.get(type, CommonAttributes.CA_CERTIFICATE_FILE, EXPRESSIONS_ALLOWED).set(true);

        node.get(type, CommonAttributes.CA_REVOCATION_URL, TYPE).set(ModelType.STRING);
        node.get(type, CommonAttributes.CA_REVOCATION_URL, DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl.ca-revocation-url"));
        node.get(type, CommonAttributes.CA_REVOCATION_URL, REQUIRED).set(false);

    }

    public static ModelNode getSSLDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();

        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);

        getSSLCommonDescription(node, ATTRIBUTES, bundle);
        return node;
    }

    public static ModelNode getModClusterAddSSL(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl-add"));

        return node;
    }

    public static ModelNode getModClusterRemoveSSL(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("modcluster.configuration.ssl-remove"));

        return node;
    }

    public static ModelNode getConfigurationDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);

        getConfigurationCommonDescription(node, ATTRIBUTES, bundle);
        return node;
    }
}
