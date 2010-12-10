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

package org.jboss.as.model;

import java.io.Serializable;
import java.util.Map;

/**
 * A generic name-value property.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Property implements Comparable<Property>, Serializable {
    private static final long serialVersionUID = 899626227115304356L;

    private final String name;
    private final String value;

    /**
     * Construct a new instance.
     *
     * @param name the property name
     * @param value the property value
     */
    public Property(final String name, final String value) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        this.name = name;
        this.value = value == null ? "" : value;
    }

    /**
     * Get the property name.
     *
     * @return the property name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the property value.
     *
     * @return the property value
     */
    public String getValue() {
        return value;
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final Object other) {
        return other instanceof Property && equals((Property)other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final Property other) {
        return this == other || other != null && other.name.equals(name) && other.value.equals(value);
    }

    /**
     * Add this property to the given map.  If the property already exists, overwrite it.
     *
     * @param map the map to add this property to
     */
    public void addTo(Map<? super String, ? super String> map) {
        map.put(name, value);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    /** {@inheritDoc} */
    public int compareTo(final Property o) {
        final int res = name.compareTo(o.name);
        return res == 0 ? value.compareTo(o.value) : res;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "property " + name + "=" + value;
    }
}
