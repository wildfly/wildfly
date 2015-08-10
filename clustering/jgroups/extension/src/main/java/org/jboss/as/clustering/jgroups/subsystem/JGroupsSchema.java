/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

/**
 * Enumeration of the supported subsystem xml schemas.
 * @author Paul Ferraro
 */
public enum JGroupsSchema {

    VERSION_1_0(1, 0),
    VERSION_1_1(1, 1),
    VERSION_2_0(2, 0),
    VERSION_3_0(3, 0),
    ;
    public static final JGroupsSchema CURRENT = VERSION_3_0;

    private final int major;
    private final int minor;

    private JGroupsSchema(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Indicates whether this version of the schema is greater than or equal to the version of the specified schema.
     * @param a schema
     * @return true, if this version of the schema is greater than or equal to the version of the specified schema, false otherwise.
     */
    public boolean since(JGroupsSchema schema) {
        return (this.major > schema.major) || ((this.major == schema.major) && (this.minor >= schema.minor));
    }

    /**
     * Get the namespace URI for this schema version.
     * @return the namespace URI
     */
    public String getNamespaceUri() {
        return this.format("urn:jboss:domain:jgroups:%d.%d");
    }

    /**
     * Formats a string using the specified pattern.
     * @param pattern a formatter pattern
     * @return a formatted string
     */
    String format(String pattern) {
        return String.format(pattern, this.major, this.minor);
    }
}
