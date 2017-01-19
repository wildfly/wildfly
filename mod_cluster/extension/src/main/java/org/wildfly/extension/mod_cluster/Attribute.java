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

/**
 * @author Emanuel Muckenhuber
 * @author Jean-Frederic Clere
 */
enum Attribute {
    UNKNOWN(null),
    LOAD_METRIC(CommonAttributes.LOAD_METRIC),
    PROXY_CONF(CommonAttributes.PROXY_CONF),
    HTTPD_CONF(CommonAttributes.HTTPD_CONF),
    NODES_CONF(CommonAttributes.NODES_CONF),
    ADVERTISE_SOCKET(CommonAttributes.ADVERTISE_SOCKET),
    SSL(CommonAttributes.SSL),
    PROXIES(CommonAttributes.PROXIES),
    PROXY_LIST(CommonAttributes.PROXY_LIST),
    PROXY_URL(CommonAttributes.PROXY_URL),
    ADVERTISE(CommonAttributes.ADVERTISE),
    ADVERTISE_SECURITY_KEY(CommonAttributes.ADVERTISE_SECURITY_KEY),
    LOAD_PROVIDER(CommonAttributes.LOAD_PROVIDER),
    SIMPLE_LOAD_PROVIDER_FACTOR(CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR),
    DYNAMIC_LOAD_PROVIDER(CommonAttributes.DYNAMIC_LOAD_PROVIDER),
    CUSTOM_LOAD_METRIC(CommonAttributes.CUSTOM_LOAD_METRIC),
    EXCLUDED_CONTEXTS(CommonAttributes.EXCLUDED_CONTEXTS),
    AUTO_ENABLE_CONTEXTS(CommonAttributes.AUTO_ENABLE_CONTEXTS),
    STOP_CONTEXT_TIMEOUT(CommonAttributes.STOP_CONTEXT_TIMEOUT),
    SOCKET_TIMEOUT(CommonAttributes.SOCKET_TIMEOUT),
    SSL_CONTEXT(CommonAttributes.SSL_CONTEXT),
    CONNECTOR(CommonAttributes.CONNECTOR),
    STATUS_INTERVAL(CommonAttributes.STATUS_INTERVAL),

    STICKY_SESSION(CommonAttributes.STICKY_SESSION),
    STICKY_SESSION_REMOVE(CommonAttributes.STICKY_SESSION_REMOVE),
    STICKY_SESSION_FORCE(CommonAttributes.STICKY_SESSION_FORCE),
    WORKER_TIMEOUT(CommonAttributes.WORKER_TIMEOUT),
    MAX_ATTEMPTS(CommonAttributes.MAX_ATTEMPTS),
    FLUSH_PACKETS(CommonAttributes.FLUSH_PACKETS),
    FLUSH_WAIT(CommonAttributes.FLUSH_WAIT),
    PING(CommonAttributes.PING),
    SMAX(CommonAttributes.SMAX),
    TTL(CommonAttributes.TTL),
    NODE_TIMEOUT(CommonAttributes.NODE_TIMEOUT),
    BALANCER(CommonAttributes.BALANCER),
    DOMAIN(CommonAttributes.DOMAIN),
    LOAD_BALANCING_GROUP(CommonAttributes.LOAD_BALANCING_GROUP),

    FACTOR(CommonAttributes.FACTOR),
    HISTORY(CommonAttributes.HISTORY),
    DECAY(CommonAttributes.DECAY),
    NAME(CommonAttributes.NAME),
    CAPACITY(CommonAttributes.CAPACITY),
    WEIGHT(CommonAttributes.WEIGHT),
    TYPE(CommonAttributes.TYPE),
    CLASS(CommonAttributes.CLASS),
    PROPERTY(CommonAttributes.PROPERTY),
    VALUE(CommonAttributes.VALUE),
    KEY_ALIAS(CommonAttributes.KEY_ALIAS),
    PASSWORD(CommonAttributes.PASSWORD),
    CERTIFICATE_KEY_FILE(CommonAttributes.CERTIFICATE_KEY_FILE),
    CIPHER_SUITE(CommonAttributes.CIPHER_SUITE),
    PROTOCOL(CommonAttributes.PROTOCOL),
    CA_CERTIFICATE_FILE(CommonAttributes.CA_CERTIFICATE_FILE),
    CA_REVOCATION_URL(CommonAttributes.CA_REVOCATION_URL),
    SESSION_DRAINING_STRATEGY(CommonAttributes.SESSION_DRAINING_STRATEGY),;

    private final String name;

    Attribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this attribute.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Attribute> MAP;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute element : values()) {
            final String name = element.getLocalName();
            if (name != null) { map.put(name, element); }
        }
        MAP = map;
    }

    public static Attribute forName(String localName) {
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    @Override
    public String toString() {
        return getLocalName();
    }
}
