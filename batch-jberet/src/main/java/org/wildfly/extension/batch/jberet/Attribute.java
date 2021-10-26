/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.jberet;

import java.util.Map;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public enum Attribute {
    UNKNOWN(null),
    DATA_SOURCE("data-source"),
    NAME("name"),
    VALUE("value"),
    EXECUTION_RECORDS_LIMIT("execution-records-limit");

    private static final Map<String, Attribute> MAP = Map.of(
            DATA_SOURCE.name, DATA_SOURCE,
            NAME.name, NAME,
            VALUE.name, VALUE,
            EXECUTION_RECORDS_LIMIT.name, EXECUTION_RECORDS_LIMIT);

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

    public static Attribute forName(String localName) {
        if (localName == null) {
            return UNKNOWN;
        }
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
