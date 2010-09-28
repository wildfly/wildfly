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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SchemaLocation implements Serializable {

    private static final long serialVersionUID = -2933175051556702331L;

    private final String namespaceUri;
    private final String locationUri;

    /**
     * Construct a new instance.
     *
     * @param namespaceUri the namespace URI (may not be {@code null})
     * @param locationUri the location URI (may not be {@code null})
     */
    public SchemaLocation(final String namespaceUri, final String locationUri) {
        if (namespaceUri == null) {
            throw new IllegalArgumentException("namespaceUri is null");
        }
        if (locationUri == null) {
            throw new IllegalArgumentException("locationUri is null");
        }
        this.namespaceUri = namespaceUri;
        this.locationUri = locationUri;
    }

    /**
     * Get the namespace URI string.
     *
     * @return the namespace URI
     */
    public String getNamespaceUri() {
        return namespaceUri;
    }

    /**
     * Get the schema location URI string.
     *
     * @return the location URI
     */
    public String getLocationUri() {
        return locationUri;
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(Object other) {
        return other instanceof SchemaLocation && equals((SchemaLocation)other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(SchemaLocation other) {
        return this == other || other != null && other.namespaceUri.equals(namespaceUri) && other.locationUri.equals(locationUri);
    }

    /**
     * Calculate the hash code for this object.
     *
     * @return the hash code
     */
    public int hashCode() {
        int result = namespaceUri.hashCode();
        result = 31 * result + locationUri.hashCode();
        return result;
    }

}
