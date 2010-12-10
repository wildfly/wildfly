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
 * Encapsulates a namespace declaration.
 *
 * @author Brian Stansberry
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class NamespacePrefix implements Serializable {

    private static final long serialVersionUID = 5356431350251733240L;

    private final String prefix;
    private final String uri;

    /**
     * Construct a new instance.
     *
     * @param prefix the namespace prefix (may not be {@code null})
     * @param uri the namespace URI (may not be {@code null})
     */
    public NamespacePrefix(String prefix, String uri) {
        if (prefix == null) {
            throw new IllegalArgumentException("prefix is null");
        }
        this.prefix = prefix;

        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }
        this.uri = uri;
    }

    /**
     * Get the namespace URI.
     *
     * @return the namespace URI
     */
    public String getNamespaceURI() {
        return uri;
    }

    /**
     * Get the namespace prefix.
     *
     * @return the namespace prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final Object other) {
        return other instanceof NamespacePrefix && equals((NamespacePrefix) other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final NamespacePrefix other) {
        return this == other || other != null && other.prefix.equals(prefix) && other.uri.equals(uri);
    }

    /**
     * Calculate the hash code for this object.
     *
     * @return the hash code
     */
    public int hashCode() {
        int result = prefix.hashCode();
        result = 31 * result + uri.hashCode();
        return result;
    }
}
