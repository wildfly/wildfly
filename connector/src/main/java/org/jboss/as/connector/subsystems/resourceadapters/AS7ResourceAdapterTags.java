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

package org.jboss.as.connector.subsystems.resourceadapters;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public enum AS7ResourceAdapterTags {
    /**
     * always first
     */
    UNKNOWN(null),

    /**
     * id tag
     */
    ID("id"),

    /**
     * config-property tag
     */
    CONFIG_PROPERTY("config-property"),

    /**
     * archive tag
     */
    ARCHIVE("archive"),

    /**
     * module tag
     */
    MODULE("module"),

    /**
     * bean-validation-groups tag
     */
    BEAN_VALIDATION_GROUPS("bean-validation-groups"),

    /**
     * bean-validation-group tag
     */
    BEAN_VALIDATION_GROUP("bean-validation-group"),

    /**
     * bootstrap-context tag
     */
    BOOTSTRAP_CONTEXT("bootstrap-context"),

    /**
     * transaction-support tag
     */
    TRANSACTION_SUPPORT("transaction-support"),
    /**
     * connection-definitions tag
     */
    CONNECTION_DEFINITIONS("connection-definitions"),
    /**
     * connection-definition tag
     */
    CONNECTION_DEFINITION("connection-definition"),

    /**
     * admin-objects tag
     */
    ADMIN_OBJECTS("admin-objects"),

    /**
     * admin-objects tag
     */
    ADMIN_OBJECT("admin-object"),

    WORKMANAGER("workmanager");

    private String name;

    /**
     * Create a new Tag.
     *
     * @param name a name
     */
    AS7ResourceAdapterTags(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return name;
    }

    private static final Map<String, AS7ResourceAdapterTags> MAP;

    static {
        final Map<String, AS7ResourceAdapterTags> map = new HashMap<String, AS7ResourceAdapterTags>();
        for (AS7ResourceAdapterTags element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    /**
     * Set the value
     *
     * @param v The name
     * @return The value
     */
    AS7ResourceAdapterTags value(String v) {
        name = v;
        return this;
    }

    /**
     * Static method to get enum instance given localName string
     *
     * @param localName a string used as localname (typically tag name as defined in xsd)
     * @return the enum instance
     */
    public static AS7ResourceAdapterTags forName(String localName) {
        final AS7ResourceAdapterTags element = MAP.get(localName);
        return element == null ? UNKNOWN.value(localName) : element;
    }

}
