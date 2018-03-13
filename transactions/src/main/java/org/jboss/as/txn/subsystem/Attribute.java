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

package org.jboss.as.txn.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of attributes used in the transactions subsystem.
 *
 * @author John E. Bailey
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
enum Attribute {
    UNKNOWN(null),
    BINDING("socket-binding"),
    STATUS_BINDING("status-socket-binding"),
    NODE_IDENTIFIER("node-identifier"),
    SOCKET_PROCESS_ID_MAX_PORTS("socket-process-id-max-ports"),
    ENABLE_STATISTICS("enable-statistics"),
    ENABLE_TSM_STATUS("enable-tsm-status"),
    DEFAULT_TIMEOUT("default-timeout"),
    MAXIMUM_TIMEOUT("maximum-timeout"),
    RECOVERY_LISTENER("recovery-listener"),
    RELATIVE_TO("relative-to"),
    STATISTICS_ENABLED("statistics-enabled"),
    PATH("path"),
    DATASOURCE_JNDI_NAME("datasource-jndi-name"),
    TABLE_PREFIX("table-prefix"),
    DROP_TABLE("drop-table"),
    ENABLE_ASYNC_IO("enable-async-io"),
    JNDI_NAME(CommonAttributes.CM_JNDI_NAME),
    CM_TABLE_IMMEDIATE_CLEANUP(CommonAttributes.CM_IMMEDIATE_CLEANUP),
    CM_TABLE_BATCH_SIZE(CommonAttributes.CM_BATCH_SIZE),
    NAME(CommonAttributes.CM_LOCATION_NAME)
    ;
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
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Attribute forName(String localName) {
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    public String toString() {
        return getLocalName();
    }
}
