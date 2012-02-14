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

import java.lang.reflect.Method;

/**
 * Immutable class which holds information about a raw sql query.
 * A raw sql query allows you to do anything sql allows you to do.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCRawSqlQueryMetaData implements JDBCQueryMetaData {
    private final Method method;

    private final Class compiler;

    private final boolean lazyResultSetLoading;

    /**
     * Constructs a JDBCRawSqlQueryMetaData which is invoked by the specified
     * method.
     *
     * @param method the method which invokes this query
     */
    public JDBCRawSqlQueryMetaData(Method method, Class<?> qlCompiler, boolean lazyResultSetLoading) {
        this.method = method;
        this.compiler = qlCompiler;
        this.lazyResultSetLoading = lazyResultSetLoading;
    }

    public Method getMethod() {
        return method;
    }

    public boolean isResultTypeMappingLocal() {
        return false;
    }

    public Class<?> getQLCompilerClass() {
        return compiler;
    }

    /**
     * Gets the read ahead metadata for the query.
     *
     * @return the read ahead metadata for the query.
     */
    public JDBCReadAheadMetaData getReadAhead() {
        return JDBCReadAheadMetaData.DEFAULT;
    }

    public boolean isLazyResultSetLoading() {
        return lazyResultSetLoading;
    }

    /**
     * Compares this JDBCRawSqlQueryMetaData against the specified object. Returns
     * true if the objects are the same. Two JDBCRawSqlQueryMetaData are the same
     * if they are both invoked by the same method.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the object argument; false otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof JDBCRawSqlQueryMetaData) {
            return ((JDBCRawSqlQueryMetaData) o).method.equals(method);
        }
        return false;
    }

    /**
     * Returns a hashcode for this JDBCRawSqlQueryMetaData. The hashcode is computed
     * by the method which invokes this query.
     *
     * @return a hash code value for this object
     */
    public int hashCode() {
        return method.hashCode();
    }

    /**
     * Returns a string describing this JDBCRawSqlQueryMetaData. The exact details
     * of the representation are unspecified and subject to change, but the following
     * may be regarded as typical:
     * <p/>
     * "[JDBCRawSqlQueryMetaData: method=public org.foo.User findByName(java.lang.String)]"
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "[JDBCRawSqlQueryMetaData : method=" + method + "]";
    }
}
