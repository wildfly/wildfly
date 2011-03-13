/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.connector.adapters.jdbc;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * JBossWrapper.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 85945 $
 */
public class JBossWrapper implements Serializable {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = -4018404397552543628L;

    /**
     * Constructor
     */
    public JBossWrapper() {
    }

    /**
     * Is a wrapper for
     *
     * @param iface The interface
     * @return True if wrapper; false otherwise
     * @throws SQLException Thrown if an error occurs
     */
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface == null)
            throw new IllegalArgumentException("Null interface");

        Object wrapped = getWrappedObject();

        if (wrapped == null)
            return false;

        return iface.isAssignableFrom(wrapped.getClass());
    }

    /**
     * Unwrap
     *
     * @param <T>   the type
     * @param iface The interface
     * @return The object
     * @throws SQLException Thrown if an error occurs
     */
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface == null)
            throw new IllegalArgumentException("Null interface");

        Object wrapped = getWrappedObject();

        if (wrapped != null && iface.isAssignableFrom(wrapped.getClass()))
            return iface.cast(wrapped);

        throw new SQLException("Not a wrapper for: " + iface.getName());
    }

    /**
     * Get the wrapped object - override in sub-classes
     *
     * @return The object
     * @throws SQLException Thrown if an error occurs
     */
    protected Object getWrappedObject() throws SQLException {
        return null;
    }
}
