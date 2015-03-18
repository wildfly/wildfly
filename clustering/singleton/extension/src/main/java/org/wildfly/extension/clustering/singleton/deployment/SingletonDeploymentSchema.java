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

package org.wildfly.extension.clustering.singleton.deployment;

import javax.xml.namespace.QName;

/**
 * Enumerates the singleton deployment configuration schemas.
 * @author Paul Ferraro
 */
public enum SingletonDeploymentSchema {

    VERSION_1_0("singleton-deployment", 1, 0),
    ;
    public static final SingletonDeploymentSchema CURRENT = VERSION_1_0;
    private static final String NAMESPACE_URI_PATTERN = "urn:jboss:%s:%d.%d";

    private final String root;
    private final int major;
    private final int minor;

    SingletonDeploymentSchema(String root, int major, int minor) {
        this.root = root;
        this.major = major;
        this.minor = minor;
    }

    /**
     * Indicates whether this version of the schema is greater than or equal to the version of the specified schema.
     * @param a schema
     * @return true, if this version of the schema is greater than or equal to the version of the specified schema, false otherwise.
     */
    public boolean since(SingletonDeploymentSchema schema) {
        return (this.major > schema.major) || ((this.major == schema.major) && (this.minor >= schema.minor));
    }

    public QName getRoot() {
        return new QName(String.format(NAMESPACE_URI_PATTERN, this.root, this.major, this.minor), this.root);
    }
}
