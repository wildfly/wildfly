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

import org.jboss.logging.Logger;

/**
 * Immutable class which holds a mapping between a Java Class and a JDBC type
 * and a SQL type.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @version $Revision: 81030 $
 */
public final class JDBCMappingMetaData {
    private static Logger log = Logger.getLogger(JDBCMappingMetaData.class.getName());

    /**
     * fully qualified Java type name
     */
    private String javaType;
    /**
     * JDBC type according to java.sql.Types
     */
    private int jdbcType;
    /**
     * SQL type
     */
    private String sqlType;
    /**
     * parameter setter
     */
    private String paramSetter;
    /**
     * result set reader
     */
    private String resultReader;

    public JDBCMappingMetaData() {

    }

    /**
     * Gets the java type of this mapping. The java type is used to differentiate
     * this mapping from other mappings.
     *
     * @return the java type of this mapping
     */
    public String getJavaType() {
        return javaType;
    }

    /**
     * Gets the jdbc type of this mapping. The jdbc type is used to retrieve data
     * from a result set and to set parameters in a prepared statement.
     *
     * @return the jdbc type of this mapping
     */
    public int getJdbcType() {
        return jdbcType;
    }

    /**
     * Gets the sql type of this mapping. The sql type is the sql column data
     * type, and is used in CREATE TABLE statements.
     *
     * @return the sql type String of this mapping
     */
    public String getSqlType() {
        return sqlType;
    }

    public String getParamSetter() {
        return paramSetter;
    }

    public String getResultReader() {
        return resultReader;
    }

    public void setJavaType(final String javaType) {
        this.javaType = javaType;
    }

    public void setJdbcType(final int jdbcType) {
        this.jdbcType = jdbcType;
    }

    public void setSqlType(final String sqlType) {
        this.sqlType = sqlType;
    }

    public void setParamSetter(final String paramSetter) {
        this.paramSetter = paramSetter;
    }

    public void setResultReader(String resultReader) {
        this.resultReader = resultReader;
    }
}
