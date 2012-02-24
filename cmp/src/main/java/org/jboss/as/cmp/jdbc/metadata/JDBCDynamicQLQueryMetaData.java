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
import org.jboss.as.cmp.CmpMessages;


/**
 * Immutable class which contains information about an DynamicQL query.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCDynamicQLQueryMetaData implements JDBCQueryMetaData {
    /**
     * The method to which this query is bound.
     */
    private final Method method;

    /**
     * Should the query return Local or Remote beans.
     */
    private final boolean resultTypeMappingLocal;

    private final JDBCReadAheadMetaData readAhead;

    private final Class<?> compiler;

    private final boolean lazyResultSetLoading;

    /**
     * Constructs a JDBCDynamicQLQueryMetaData with DynamicQL declared in the
     * jboss-ql element and is invoked by the specified method.
     *
     * @param defaults the metadata about this query
     */
    public JDBCDynamicQLQueryMetaData(JDBCDynamicQLQueryMetaData defaults,
                                      JDBCReadAheadMetaData readAhead,
                                      Class<?> qlCompiler,
                                      boolean lazyResultSetLoading) {
        this.method = defaults.getMethod();
        this.readAhead = readAhead;
        this.resultTypeMappingLocal = defaults.isResultTypeMappingLocal();
        compiler = qlCompiler;
        this.lazyResultSetLoading = lazyResultSetLoading;
    }


    /**
     * Constructs a JDBCDynamicQLQueryMetaData with DynamicQL declared in the
     * jboss-ql element and is invoked by the specified method.
     */
    public JDBCDynamicQLQueryMetaData(boolean resultTypeMappingLocal,
                                      Method method,
                                      JDBCReadAheadMetaData readAhead,
                                      Class<?> compiler,
                                      boolean lazyResultSetLoading) {

        this.method = method;
        this.readAhead = readAhead;
        this.resultTypeMappingLocal = resultTypeMappingLocal;

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 2
                ||
                !parameterTypes[0].equals(String.class) ||
                !parameterTypes[1].equals(Object[].class)) {
            throw CmpMessages.MESSAGES.dynamicQlInvalidParameters();
        }

        this.compiler = compiler;
        this.lazyResultSetLoading = lazyResultSetLoading;
    }

    // javadoc in parent class
    public Method getMethod() {
        return method;
    }

    // javadoc in parent class
    public boolean isResultTypeMappingLocal() {
        return resultTypeMappingLocal;
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
     * Compares this JDBCDynamicQLQueryMetaData against the specified object.
     * Returns true if the objects are the same. Two JDBCDynamicQLQueryMetaData
     * are the same if they are both invoked by the same method.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the object argument;
     *         false otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof JDBCDynamicQLQueryMetaData) {
            return ((JDBCDynamicQLQueryMetaData) o).method.equals(method);
        }
        return false;
    }

    /**
     * Returns a hashcode for this JDBCDynamicQLQueryMetaData. The hashcode is
     * computed by the method which invokes this query.
     *
     * @return a hash code value for this object
     */
    public int hashCode() {
        return method.hashCode();
    }

    /**
     * Returns a string describing this JDBCDynamicQLQueryMetaData. The exact
     * details of the representation are unspecified and subject to change, but
     * the following may be regarded as typical:
     * <p/>
     * "[JDBCDynamicQLQueryMetaData: method=public org.foo.User
     * findByName(java.lang.String)]"
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "[JDBCDynamicQLQueryMetaData : method=" + method + "]";
    }
}
