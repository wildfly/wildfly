/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.jca;

import java.util.HashMap;
import java.util.Map;

/**
 * An Attribute.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public enum Attribute {
    /** always the first **/
    UNKNOWN(null),

    ENABLED("enabled"),
    /**
     * fail-on-error attribute
     */
    FAIL_ON_ERROR("fail-on-error"),

    /**
     * fail-on-warn attribute
     */
    FAIL_ON_WARN("fail-on-warn"),

    SHORT_RUNNING_THREAD_POOL("short-running-thread-pool"),

    LONG_RUNNING_THREAD_POOL("long-running-thread-pool"),

    DEBUG("debug"),

    ERROR("error"),

    IGNORE_UNKNOWN_CONNECHIONS("ignore-unknown-connections"),

    NAME("name"),

    WORKMANAGER("workmanager"),

    JGROUPS_STACK("jgroups-stack"),

    JGROUPS_CLUSTER("jgroups-cluster"),

    REQUEST_TIMEOUT("request-timeout");

    private final String name;

    Attribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
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
            if (name != null)
                map.put(name, element);
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
