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
 * This immutable class contains information about an automatically generated
 * query. This class is a place holder used to make an automatically generated
 * query look more like a user specified query.  This class only contains a
 * reference to the method used to invoke this query.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @version $Revision: 81030 $
 */
public final class JDBCAutomaticQueryMetaData implements JDBCQueryMetaData {
    /**
     * A reference to the method which invokes this query.
     */
    private final Method method;

    /**
     * Read ahead meta data.
     */
    private final JDBCReadAheadMetaData readAhead;

    private final Class compiler;

    private final boolean lazyResultSetLoading;

    /**
     * Constructs a JDBCAutomaticQueryMetaData which is invoked by the specified
     * method.
     *
     * @param method the method which invokes this query
     * @readAhead Read ahead meta data.
     */
    public JDBCAutomaticQueryMetaData(Method method, JDBCReadAheadMetaData readAhead, Class qlCompiler, boolean lazyResultSetLoading) {
        this.method = method;
        this.readAhead = readAhead;
        this.compiler = qlCompiler;
        this.lazyResultSetLoading = lazyResultSetLoading;
    }

    public Method getMethod() {
        return method;
    }

    public boolean isResultTypeMappingLocal() {
        return false;
    }

    /**
     * Gets the read ahead metadata for the query.
     *
     * @return the read ahead metadata for the query.
     */
    public JDBCReadAheadMetaData getReadAhead() {
        return readAhead;
    }

    public Class getQLCompilerClass() {
        return compiler;
    }

    public boolean isLazyResultSetLoading() {
        return lazyResultSetLoading;
    }

    /**
     * Compares this JDBCAutomaticQueryMetaData against the specified object. Returns
     * true if the objects are the same. Two JDBCAutomaticQueryMetaData are the same
     * if they are both invoked by the same method.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the object argument; false otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof JDBCAutomaticQueryMetaData) {
            return ((JDBCAutomaticQueryMetaData) o).method.equals(method);
        }
        return false;
    }

    /**
     * Returns a hashcode for this JDBCAutomaticQueryMetaData. The hashcode is computed
     * by the method which invokes this query.
     *
     * @return a hash code value for this object
     */
    public int hashCode() {
        return method.hashCode();
    }

    /**
     * Returns a string describing this JDBCAutomaticQueryMetaData. The exact details
     * of the representation are unspecified and subject to change, but the following
     * may be regarded as typical:
     * <p/>
     * "[JDBCAutomaticQueryMetaData: method=public org.foo.User findByName(java.lang.String)]"
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "[JDBCAutomaticQueryMetaData : method=" + method + "]";
    }
}
