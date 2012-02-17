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

import org.jboss.metadata.ejb.spec.QueryMetaData;
import org.jboss.metadata.ejb.spec.ResultTypeMapping;

/**
 * Immutable class which contains information about an EJB QL query.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCQlQueryMetaData implements JDBCQueryMetaData {
    /**
     * The method to which this query is bound.
     */
    private final Method method;

    /**
     * The ejb-ql fro the query.
     */
    private final String ejbQl;

    /**
     * Should the query return Local or Remote beans.
     */
    private final boolean resultTypeMappingLocal;

    /**
     * Read ahead meta data.
     */
    private final JDBCReadAheadMetaData readAhead;

    private final Class<?> compilerClass;

    private final boolean lazyResultSetLoading;

    /**
     * Constructs a JDBCQlQueryMetaData which is defined by the queryMetaData
     * and is invoked by the specified method.
     *
     * @param queryMetaData the metadata about this query which was loaded
     *                      from the ejb-jar.xml file
     * @param method        the method which invokes this query
     */
    public JDBCQlQueryMetaData(QueryMetaData queryMetaData, Method method, Class<?> qlCompiler, boolean lazyResultSetLoading) {
        this.method = method;
        this.readAhead = JDBCReadAheadMetaData.DEFAULT;
        ejbQl = queryMetaData.getEjbQL();
        resultTypeMappingLocal = queryMetaData.getResultTypeMapping().equals(ResultTypeMapping.Local);

        compilerClass = qlCompiler;
        this.lazyResultSetLoading = lazyResultSetLoading;
    }

    /**
     * Constructs a JDBCQlQueryMetaData with data from the jdbcQueryMetaData
     * and additional data from the xml element
     */
    public JDBCQlQueryMetaData(JDBCQlQueryMetaData defaults, JDBCReadAheadMetaData readAhead, Class<?> compilerClass, boolean lazyResultSetLoading) {
        this.method = defaults.getMethod();
        this.readAhead = readAhead;
        this.ejbQl = defaults.getEjbQl();
        this.resultTypeMappingLocal = defaults.resultTypeMappingLocal;
        this.compilerClass = compilerClass;
        this.lazyResultSetLoading = lazyResultSetLoading;
    }


    /**
     * Constructs a JDBCQlQueryMetaData with data from the jdbcQueryMetaData
     * and additional data from the xml element
     */
    public JDBCQlQueryMetaData(JDBCQlQueryMetaData jdbcQueryMetaData,
                               Method method,
                               JDBCReadAheadMetaData readAhead) {
        this.method = method;
        this.readAhead = readAhead;
        ejbQl = jdbcQueryMetaData.getEjbQl();
        resultTypeMappingLocal = jdbcQueryMetaData.resultTypeMappingLocal;
        compilerClass = jdbcQueryMetaData.compilerClass;
        lazyResultSetLoading = jdbcQueryMetaData.lazyResultSetLoading;
    }

    // javadoc in parent class
    public Method getMethod() {
        return method;
    }

    public Class<?> getQLCompilerClass() {
        return compilerClass;
    }

    /**
     * Gets the EJB QL query which will be invoked.
     *
     * @return the ejb ql String for this query
     */
    public String getEjbQl() {
        return ejbQl;
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

    public boolean isLazyResultSetLoading() {
        return lazyResultSetLoading;
    }

    /**
     * Compares this JDBCQlQueryMetaData against the specified object. Returns
     * true if the objects are the same. Two JDBCQlQueryMetaData are the same
     * if they are both invoked by the same method.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the object argument;
     *         false otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof JDBCQlQueryMetaData) {
            return ((JDBCQlQueryMetaData) o).method.equals(method);
        }
        return false;
    }

    /**
     * Returns a hashcode for this JDBCQlQueryMetaData. The hashcode is computed
     * by the method which invokes this query.
     *
     * @return a hash code value for this object
     */
    public int hashCode() {
        return method.hashCode();
    }

    /**
     * Returns a string describing this JDBCQlQueryMetaData. The exact details
     * of the representation are unspecified and subject to change, but the
     * following may be regarded as typical:
     * <p/>
     * "[JDBCQlQueryMetaData: method=public org.foo.User
     * findByName(java.lang.String)]"
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "[JDBCQlQueryMetaData : method=" + method + "]";
    }
}
