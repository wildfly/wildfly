/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.wildfly.extension.mod_cluster;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * Enumeration of XML attributes used solely by {@link ModClusterSubsystemXMLReader}.
 *
 * @author Emanuel Muckenhuber
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
@SuppressWarnings("deprecation")
enum XMLAttribute {
    UNKNOWN((String) null),

    // Proxy configuration
    ADVERTISE(ProxyConfigurationResourceDefinition.Attribute.ADVERTISE),
    ADVERTISE_SECURITY_KEY(ProxyConfigurationResourceDefinition.Attribute.ADVERTISE_SECURITY_KEY),
    ADVERTISE_SOCKET(ProxyConfigurationResourceDefinition.Attribute.ADVERTISE_SOCKET),
    AUTO_ENABLE_CONTEXTS(ProxyConfigurationResourceDefinition.Attribute.AUTO_ENABLE_CONTEXTS),
    BALANCER(ProxyConfigurationResourceDefinition.Attribute.BALANCER),
    @Deprecated CONNECTOR(ProxyConfigurationResourceDefinition.DeprecatedAttribute.CONNECTOR),
    @Deprecated DOMAIN("domain"),
    EXCLUDED_CONTEXTS(ProxyConfigurationResourceDefinition.Attribute.EXCLUDED_CONTEXTS),
    FLUSH_PACKETS(ProxyConfigurationResourceDefinition.Attribute.FLUSH_PACKETS),
    FLUSH_WAIT(ProxyConfigurationResourceDefinition.Attribute.FLUSH_WAIT),
    LISTENER(ProxyConfigurationResourceDefinition.Attribute.LISTENER),
    LOAD_BALANCING_GROUP(ProxyConfigurationResourceDefinition.Attribute.LOAD_BALANCING_GROUP),
    MAX_ATTEMPTS(ProxyConfigurationResourceDefinition.Attribute.MAX_ATTEMPTS),
    NAME(ModelDescriptionConstants.NAME),
    NODE_TIMEOUT(ProxyConfigurationResourceDefinition.Attribute.NODE_TIMEOUT),
    PING(ProxyConfigurationResourceDefinition.Attribute.PING),
    PROXIES(ProxyConfigurationResourceDefinition.Attribute.PROXIES),
    PROXY_LIST(ProxyConfigurationResourceDefinition.Attribute.PROXY_LIST),
    PROXY_URL(ProxyConfigurationResourceDefinition.Attribute.PROXY_URL),
    SESSION_DRAINING_STRATEGY(ProxyConfigurationResourceDefinition.Attribute.SESSION_DRAINING_STRATEGY),
    SMAX(ProxyConfigurationResourceDefinition.Attribute.SMAX),
    SOCKET_TIMEOUT(ProxyConfigurationResourceDefinition.Attribute.SOCKET_TIMEOUT),
    SSL_CONTEXT(ProxyConfigurationResourceDefinition.Attribute.SSL_CONTEXT),
    STATUS_INTERVAL(ProxyConfigurationResourceDefinition.Attribute.STATUS_INTERVAL),
    STICKY_SESSION(ProxyConfigurationResourceDefinition.Attribute.STICKY_SESSION),
    STICKY_SESSION_FORCE(ProxyConfigurationResourceDefinition.Attribute.STICKY_SESSION_FORCE),
    STICKY_SESSION_REMOVE(ProxyConfigurationResourceDefinition.Attribute.STICKY_SESSION_REMOVE),
    STOP_CONTEXT_TIMEOUT(ProxyConfigurationResourceDefinition.Attribute.STOP_CONTEXT_TIMEOUT),
    TTL(ProxyConfigurationResourceDefinition.Attribute.TTL),
    WORKER_TIMEOUT(ProxyConfigurationResourceDefinition.Attribute.WORKER_TIMEOUT),

    // Load provider
    FACTOR(SimpleLoadProviderResourceDefinition.Attribute.FACTOR),
    DECAY(DynamicLoadProviderResourceDefinition.Attribute.DECAY),
    HISTORY(DynamicLoadProviderResourceDefinition.Attribute.HISTORY),

    // Load metrics
    CAPACITY(LoadMetricResourceDefinition.SharedAttribute.CAPACITY),
    CLASS(CustomLoadMetricResourceDefinition.Attribute.CLASS),
    TYPE(LoadMetricResourceDefinition.Attribute.TYPE),
    WEIGHT(LoadMetricResourceDefinition.SharedAttribute.WEIGHT),

    // Legacy SSL
    CA_CERTIFICATE_FILE(SSLResourceDefinition.Attribute.CA_CERTIFICATE_FILE),
    CA_REVOCATION_URL(SSLResourceDefinition.Attribute.CA_REVOCATION_URL),
    CERTIFICATE_KEY_FILE(SSLResourceDefinition.Attribute.CERTIFICATE_KEY_FILE),
    CIPHER_SUITE(SSLResourceDefinition.Attribute.CIPHER_SUITE),
    KEY_ALIAS(SSLResourceDefinition.Attribute.KEY_ALIAS),
    PASSWORD(SSLResourceDefinition.Attribute.PASSWORD),
    PROTOCOL(SSLResourceDefinition.Attribute.PROTOCOL),
    ;

    private final String name;

    XMLAttribute(String name) {
        this.name = name;
    }

    XMLAttribute(Attribute attribute) {
        this.name = attribute.getName();
    }

    public String getLocalName() {
        return name;
    }

    private static final Map<String, XMLAttribute> MAP;

    static {
        Map<String, XMLAttribute> map = new HashMap<>(XMLAttribute.values().length);
        for (XMLAttribute element : values()) {
            String name = element.getLocalName();
            if (name != null) {
                map.put(name, element);
            }
        }
        MAP = map;
    }

    public static XMLAttribute forName(String localName) {
        XMLAttribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    @Override
    public String toString() {
        return this.getLocalName();
    }
}
