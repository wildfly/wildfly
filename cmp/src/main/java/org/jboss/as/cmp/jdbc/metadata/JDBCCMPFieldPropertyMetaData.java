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

/**
 * This immutable class contains information about the an overridden field property.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCCMPFieldPropertyMetaData {
    /**
     * the cmp field on which this property is defined
     */
    private JDBCCMPFieldMetaData cmpField;

    /**
     * name of this property
     */
    private String propertyName;

    /**
     * the column name in the table
     */
    private String columnName;

    /**
     * the jdbc type (see java.sql.Types), used in PreparedStatement.setParameter
     */
    private int jdbcType;

    /**
     * the sql type, used for table creation.
     */
    private String sqlType;

    /**
     * Should null values not be allowed for this property.
     */
    private boolean notNull;

    /**
     * Gets the name of the property to be overridden.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Gets the column name the property should use or null if the
     * column name is not overridden.
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Gets the JDBC type the property should use or Integer.MIN_VALUE
     * if not overridden.
     */
    public int getJDBCType() {
        return jdbcType;
    }

    /**
     * Gets the SQL type the property should use or null
     * if not overridden.
     */
    public String getSQLType() {
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
     * Compares this JDBCCMPFieldPropertyMetaData against the specified object.
     * Returns true if the objects are the same. Two
     * JDBCCMPFieldPropertyMetaData are the same if they both have the same name
     * and are defined on the same cmpField.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the object argument; false
     *         otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof JDBCCMPFieldPropertyMetaData) {
            JDBCCMPFieldPropertyMetaData cmpFieldProperty =
                    (JDBCCMPFieldPropertyMetaData) o;
            return propertyName.equals(cmpFieldProperty.propertyName) &&
                    cmpField.equals(cmpFieldProperty.cmpField);
        }
        return false;
    }

    /**
     * Returns a hashcode for this JDBCCMPFieldPropertyMetaData. The hashcode is
     * computed based on the hashCode of the declaring entity and the hashCode
     * of the fieldName
     *
     * @return a hash code value for this object
     */
    public int hashCode() {
        int result = 17;
        result = 37 * result + cmpField.hashCode();
        result = 37 * result + propertyName.hashCode();
        return result;
    }

    /**
     * Returns a string describing this JDBCCMPFieldPropertyMetaData. The exact
     * details of the representation are unspecified and subject to change, but
     * the following may be regarded as typical:
     * <p/>
     * "[JDBCCMPFieldPropertyMetaData: propertyName=line1,
     * [JDBCCMPFieldMetaData: fieldName=address,
     * [JDBCEntityMetaData: entityName=UserEJB]]"
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "[JDBCCMPFieldPropertyMetaData : propertyName=" +
                propertyName + ", " + cmpField + "]";
    }

    public void setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
    }

    public void setColumnName(final String columnName) {
        this.columnName = columnName;
    }

    public void setNotNul(final boolean notNul) {
        this.notNull = notNull;
    }

    public void setSqlType(final String elementText) {
        this.sqlType = sqlType;
    }

    public void setJdbcType(final int jdbcType) {
        this.jdbcType = jdbcType;
    }

    public void setCmpField(JDBCCMPFieldMetaData cmpField) {
        this.cmpField = cmpField;
    }
}
