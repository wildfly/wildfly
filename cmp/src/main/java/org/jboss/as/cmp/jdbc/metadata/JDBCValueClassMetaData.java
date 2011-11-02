/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.jdbc.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which holds a list of the properties for a dependent value
 * class.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCValueClassMetaData {
    private Class<?> javaType;
    private List<JDBCValuePropertyMetaData> properties = new ArrayList<JDBCValuePropertyMetaData>();

    /**
     * Gets the Java Class of this value class.
     *
     * @return the java Class of this value class
     */
    public Class<?> getJavaType() {
        return javaType;
    }

    /**
     * Gets the properties of this value class which are to be saved into the database.
     *
     * @return an unmodifiable list which contains the JDBCValuePropertyMetaData objects
     */
    public List<JDBCValuePropertyMetaData> getProperties() {
        return properties;
    }

    public void setClass(final Class<?> className) {
        this.javaType = className;
    }

    public void addProperty(final JDBCValuePropertyMetaData propertyMetaData) {
        properties.add(propertyMetaData);
    }
}
