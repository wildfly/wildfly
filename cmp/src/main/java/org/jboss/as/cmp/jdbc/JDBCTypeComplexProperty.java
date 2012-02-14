/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cmp.jdbc;

import java.lang.reflect.Method;

/**
 * Immutable class which contains the mapping between a single Java Bean
 * (not an EJB) property and a column. This class has a flattened view of
 * the Java Bean property, which may be several properties deep in the
 * base Java Bean. The details of how a property is mapped to a column
 * can be found in JDBCTypeFactory.
 * <p/>
 * This class holds a description of the column and, knows how to extract
 * the column value from the Java Bean and how to set a column value info
 * the Java Bean.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCTypeComplexProperty {
    private final String propertyName;
    private final String columnName;
    private final Class javaType;
    private final int jdbcType;
    private final String sqlType;
    private final boolean notNull;
    private final JDBCResultSetReader resultSetReader;
    private final JDBCParameterSetter paramSetter;

    private final Method[] getters;
    private final Method[] setters;

    public JDBCTypeComplexProperty(
            String propertyName,
            String columnName,
            Class javaType,
            int jdbcType,
            String sqlType,
            boolean notNull,
            Method[] getters,
            Method[] setters) {

        this.propertyName = propertyName;
        this.columnName = columnName;
        this.javaType = javaType;
        this.jdbcType = jdbcType;
        this.sqlType = sqlType;
        this.notNull = notNull;
        this.getters = getters;
        this.setters = setters;
        this.resultSetReader = JDBCUtil.getResultSetReader(jdbcType, javaType);
        this.paramSetter = JDBCUtil.getParameterSetter(jdbcType, javaType);
    }

    public JDBCTypeComplexProperty(
            JDBCTypeComplexProperty defaultProperty,
            String columnName,
            int jdbcType,
            String sqlType,
            boolean notNull) {

        this.propertyName = defaultProperty.propertyName;
        this.columnName = columnName;
        this.javaType = defaultProperty.javaType;
        this.jdbcType = jdbcType;
        this.sqlType = sqlType;
        this.notNull = notNull;
        this.getters = defaultProperty.getters;
        this.setters = defaultProperty.setters;
        this.resultSetReader = JDBCUtil.getResultSetReader(jdbcType, javaType);
        this.paramSetter = JDBCUtil.getParameterSetter(jdbcType, javaType);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getColumnName() {
        return columnName;
    }

    public Class getJavaType() {
        return javaType;
    }

    public int getJDBCType() {
        return jdbcType;
    }

    public String getSQLType() {
        return sqlType;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public JDBCResultSetReader getResultSetReader() {
        return resultSetReader;
    }

    public JDBCParameterSetter getParameterSetter() {
        return paramSetter;
    }

    public Object getColumnValue(Object value) throws Exception {
        Object[] noArgs = new Object[0];

        for (int i = 0; i < getters.length; i++) {
            if (value == null) {
                return null;
            }
            value = getters[i].invoke(value, noArgs);
        }
        return value;
    }

    public Object setColumnValue(
            Object value,
            Object columnValue) throws Exception {

        // Used for invocation of get and set
        Object[] noArgs = new Object[0];
        Object[] singleArg = new Object[1];

        // save the first value to return
        Object returnValue = value;

        // get the second to last object in the chain
        for (int i = 0; i < getters.length - 1; i++) {
            // get the next object in chain
            Object next = getters[i].invoke(value, noArgs);

            // the next object is null create it
            if (next == null) {
                // new type based on getter
                next = getters[i].getReturnType().newInstance();

                // and set it into the current value
                singleArg[0] = next;

                setters[i].invoke(value, singleArg);
            }

            // update value to the next in chain
            value = next;
        }

        // value is now the object on which we need to set the column value
        singleArg[0] = columnValue;
        setters[setters.length - 1].invoke(value, singleArg);

        // return the first object in call chain
        return returnValue;
    }
}
