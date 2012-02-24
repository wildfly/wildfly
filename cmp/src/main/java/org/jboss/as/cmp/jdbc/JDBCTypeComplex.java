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

import java.util.HashMap;
import javax.ejb.EJBException;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;

/**
 * JDBCTypeComplex provides the mapping between a Java Bean (not an EJB)
 * and a set of columns. This class has a flattened view of the Java Bean,
 * which may contain other Java Beans.  This class simply treats the bean
 * as a set of properties, which may be in the a.b.c style. The details
 * of how this mapping is performed can be found in JDBCTypeFactory.
 * <p/>
 * This class holds a description of the columns
 * and the properties that map to the columns. Additionally, this class
 * knows how to extract a column value from the Java Bean and how to set
 * a column value info the Java Bean. See JDBCTypeComplexProperty for
 * details on how this is done.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCTypeComplex implements JDBCType {
    private final JDBCTypeComplexProperty[] properties;
    private final String[] columnNames;
    private final Class[] javaTypes;
    private final int[] jdbcTypes;
    private final String[] sqlTypes;
    private final boolean[] notNull;
    private final JDBCResultSetReader[] resultSetReaders;
    private final JDBCParameterSetter[] paramSetters;
    private final Class fieldType;
    private final HashMap propertiesByName = new HashMap();

    public JDBCTypeComplex(
            JDBCTypeComplexProperty[] properties,
            Class fieldType) {

        this.properties = properties;
        this.fieldType = fieldType;

        int propNum = properties.length;
        columnNames = new String[propNum];
        javaTypes = new Class[propNum];
        jdbcTypes = new int[propNum];
        sqlTypes = new String[propNum];
        notNull = new boolean[propNum];
        resultSetReaders = new JDBCResultSetReader[propNum];
        paramSetters = new JDBCParameterSetter[propNum];
        for (int i = 0; i < properties.length; i++) {
            JDBCTypeComplexProperty property = properties[i];
            columnNames[i] = property.getColumnName();
            javaTypes[i] = property.getJavaType();
            jdbcTypes[i] = property.getJDBCType();
            sqlTypes[i] = property.getSQLType();
            notNull[i] = property.isNotNull();
            resultSetReaders[i] = property.getResultSetReader();
            paramSetters[i] = property.getParameterSetter();
            propertiesByName.put(property.getPropertyName(), property);
        }
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public Class[] getJavaTypes() {
        return javaTypes;
    }

    public int[] getJDBCTypes() {
        return jdbcTypes;
    }

    public String[] getSQLTypes() {
        return sqlTypes;
    }

    public boolean[] getNotNull() {
        return notNull;
    }

    public boolean[] getAutoIncrement() {
        return new boolean[]{false};
    }

    public Object getColumnValue(int index, Object value) {
        return getColumnValue(properties[index], value);
    }

    public Object setColumnValue(int index, Object value, Object columnValue) {
        return setColumnValue(properties[index], value, columnValue);
    }

    public boolean hasMapper() {
        return false;
    }

    public boolean isSearchable() {
        return false;
    }

    public JDBCResultSetReader[] getResultSetReaders() {
        return resultSetReaders;
    }

    public JDBCParameterSetter[] getParameterSetter() {
        return paramSetters;
    }

    public JDBCTypeComplexProperty[] getProperties() {
        return properties;
    }

    public JDBCTypeComplexProperty getProperty(String propertyName) {
        JDBCTypeComplexProperty prop = (JDBCTypeComplexProperty) propertiesByName.get(propertyName);
        if (prop == null) {
            throw CmpMessages.MESSAGES.fieldDoesNotHaveProperty(fieldType.getName(), propertyName);
        }
        return prop;
    }

    private static Object getColumnValue(JDBCTypeComplexProperty property, Object value) {
        try {
            return property.getColumnValue(value);
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw MESSAGES.errorGettingColumnValue(e);
        }
    }

    private Object setColumnValue(
            JDBCTypeComplexProperty property,
            Object value,
            Object columnValue) {

        if (value == null && columnValue == null) {
            // nothing to do
            return null;
        }

        try {
            if (value == null) {
                value = fieldType.newInstance();
            }
            return property.setColumnValue(value, columnValue);
        } catch (Exception e) {
            throw MESSAGES.errorSettingColumnValue(e);
        }
    }
}
