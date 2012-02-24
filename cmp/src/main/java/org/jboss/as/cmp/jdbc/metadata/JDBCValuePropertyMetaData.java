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
 * Class which contains information about a single dependent
 * value object property.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCValuePropertyMetaData {
    private String propertyName;
    private Class<?> propertyType;
    private String columnName;
    private String sqlType;
    private int jdbcType;
    private boolean notNull;
    private Method getter;
    private Method setter;

    /**
     * Gets the name of this property. The name will always begin with a lower
     * case letter and is a Java Beans property name.  This is the base name of
     * the getter and setter property.
     *
     * @return the name of this property
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Gets the java class type of this property. The class the the return type
     * of the getter and type of the sole argument of the setter.
     *
     * @return the java Class type of this property
     */
    public Class<?> getPropertyType() {
        return propertyType;
    }

    /**
     * Gets the column name which this property will be persisted.
     *
     * @return the name of the column which this property will be persisted
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Gets the jdbc type of this property. The jdbc type is used to retrieve
     * data from a result set and to set parameters in a prepared statement.
     *
     * @return the jdbc type of this property
     */
    public int getJDBCType() {
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

    /**
     * Should this field allow null values?
     *
     * @return true if this field will not allow a null value.
     */
    public boolean isNotNull() {
        return notNull;
    }

    /**
     * Gets the getter method of this property. The getter method is used to
     * retrieve the value of this property from the value class.
     *
     * @return the Method which gets the value of this property
     */
    public Method getGetter() {
        return getter;
    }

    /**
     * Gets the setter method of this property. The setter method is used to
     * set the value of this property in the value class.
     *
     * @return the Method which sets the value of this property
     */
    public Method getSetter() {
        return setter;
    }

    private static String toGetterName(String propertyName) {
        return "get" + upCaseFirstCharacter(propertyName);
    }

    private static String toSetterName(String propertyName) {
        return "set" + upCaseFirstCharacter(propertyName);
    }

    private static String upCaseFirstCharacter(String propertyName) {
        return Character.toUpperCase(propertyName.charAt(0)) +
                propertyName.substring(1);
    }

    public void setPropertyName(final String propertyName, final Class<?> classType) {
        this.propertyName = propertyName;

        // Getter
        try {
            getter = classType.getMethod(toGetterName(propertyName), new Class[0]);
        } catch (Exception e) {
            throw CmpMessages.MESSAGES.getterNotFoundForValue(propertyName, classType.getName());
        }

        // get property type from getter return type
        propertyType = getter.getReturnType();

        // resolve setter
        try {
            setter = classType.getMethod(
                    toSetterName(propertyName),
                    new Class[]{propertyType});
        } catch (Exception e) {
            throw CmpMessages.MESSAGES.setterNotFoundForValue(propertyName, classType.getName());
        }

    }

    public void setColumnName(final String columnName) {
        this.columnName = columnName;
    }

    public void setNotNul(final boolean notNull) {
        this.notNull = notNull;
    }

    public void setJdbcType(final int jdbcType) {
        this.jdbcType = jdbcType;
    }

    public void setSqlType(final String sqlType) {
        this.sqlType = sqlType;
    }
}
