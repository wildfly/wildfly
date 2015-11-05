/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

/**
 * Defines an XML schema version.
 * @author Paul Ferraro
 */
public interface Schema<S extends Schema<S>> {
    int major();
    int minor();

    /**
     * Get the namespace URI of this schema.
     * @return the namespace URI
     */
    String getNamespaceUri();

    /**
     * Indicates whether this version of the schema is greater than or equal to the version of the specified schema.
     * @param schema a schema version with which to compare
     * @return true, if this version of the schema is greater than or equal to the version of the specified schema, false otherwise.
     */
    default boolean since(S schema) {
        return (this.major() > schema.major()) || ((this.major() == schema.major()) && (this.minor() >= schema.minor()));
    }
}
