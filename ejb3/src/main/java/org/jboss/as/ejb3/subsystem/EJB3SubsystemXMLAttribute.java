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
package org.jboss.as.ejb3.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jaikiran Pai
 */
public enum EJB3SubsystemXMLAttribute {
    UNKNOWN(null),

    ALIASES("aliases"),

    BEAN_CACHE("bean-cache"),

    CACHE_CONTAINER("cache-container"),
    CACHE_REF("cache-ref"),
    CLIENT_MAPPINGS_CACHE("client-mappings-cache"),
    CLUSTERED_CACHE_REF("clustered-cache-ref"),
    CONNECTOR_REF("connector-ref"),
    CORE_THREADS("core-threads"),

    DEFAULT_ACCESS_TIMEOUT("default-access-timeout"),

    ENABLED("enabled"),
    ENABLE_BY_DEFAULT("enable-by-default"),

    GROUPS_PATH("groups-path"),

    IDLE_TIMEOUT("idle-timeout"),
    IDLE_TIMEOUT_UNIT("idle-timeout-unit"),
    INSTANCE_AQUISITION_TIMEOUT("instance-acquisition-timeout"),
    INSTANCE_AQUISITION_TIMEOUT_UNIT("instance-acquisition-timeout-unit"),

    KEEPALIVE_TIME("keepalive-time"),

    MAX_POOL_SIZE("max-pool-size"),
    MAX_SIZE("max-size"),
    MAX_THREADS("max-threads"),

    NAME("name"),

    PASSIVATE_EVENTS_ON_REPLICATE("passivate-events-on-replicate"),
    PASSIVATION_STORE_REF("passivation-store-ref"),
    PATH("path"),
    POOL_NAME("pool-name"),

    RELATIVE_TO("relative-to"),
    RESOURCE_ADAPTER_NAME("resource-adapter-name"),

    SESSIONS_PATH("sessions-path"),
    SUBDIRECTORY_COUNT("subdirectory-count"),

    THREAD_POOL_NAME("thread-pool-name"),

    USE_QUALIFIED_NAME("use-qualified-name"),
    ;

    private final String name;

    EJB3SubsystemXMLAttribute(final String name) {
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

    private static final Map<String, EJB3SubsystemXMLAttribute> MAP;

    static {
        final Map<String, EJB3SubsystemXMLAttribute> map = new HashMap<String, EJB3SubsystemXMLAttribute>();
        for (EJB3SubsystemXMLAttribute element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static EJB3SubsystemXMLAttribute forName(String localName) {
        final EJB3SubsystemXMLAttribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    @Override
    public String toString() {
        return getLocalName();
    }
}
