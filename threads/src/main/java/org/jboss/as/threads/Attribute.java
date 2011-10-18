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

package org.jboss.as.threads;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum Attribute {
    UNKNOWN(null),
    /* Threads 1.0 attributes, in alpha order */
    ALLOW_CORE_TIMEOUT(CommonAttributes.ALLOW_CORE_TIMEOUT),
    BLOCKING(CommonAttributes.BLOCKING),
    CORE_THREADS(CommonAttributes.CORE_THREADS),
    COUNT(CommonAttributes.COUNT),
    GROUP_NAME(CommonAttributes.GROUP_NAME),
    MAX_THREADS(CommonAttributes.MAX_THREADS),
    NAME(CommonAttributes.NAME),
    PER_CPU(CommonAttributes.PER_CPU),
    PRIORITY(CommonAttributes.PRIORITY),
    QUEUE_LENGTH(CommonAttributes.QUEUE_LENGTH),
    THREAD_NAME_PATTERN(CommonAttributes.THREAD_NAME_PATTERN),
    TIME(CommonAttributes.TIME),
    UNIT(CommonAttributes.UNIT),
    VALUE(CommonAttributes.VALUE),
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
