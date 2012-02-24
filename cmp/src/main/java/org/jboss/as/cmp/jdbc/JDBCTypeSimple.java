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

import java.sql.Types;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;


/**
 * This class provides a simple mapping of a Java type type to a single column.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCTypeSimple implements JDBCType {
    private final String[] columnNames;
    private final Class[] javaTypes;
    private final int[] jdbcTypes;
    private final String[] sqlTypes;
    private final boolean[] notNull;
    private final boolean[] autoIncrement;
    private final JDBCResultSetReader[] resultSetReader;
    private final JDBCParameterSetter[] paramSetter;

    private final Mapper mapper;

    public JDBCTypeSimple(
            String columnName,
            Class javaType,
            int jdbcType,
            String sqlType,
            boolean notNull,
            boolean autoIncrement,
            Mapper mapper,
            JDBCParameterSetter paramSetter,
            JDBCResultSetReader resultReader
    ) {
        columnNames = new String[]{columnName};
        javaTypes = new Class[]{javaType};
        jdbcTypes = new int[]{jdbcType};
        sqlTypes = new String[]{sqlType};
        this.notNull = new boolean[]{notNull};
        this.autoIncrement = new boolean[]{autoIncrement};
        this.mapper = mapper;
        resultSetReader = new JDBCResultSetReader[]{resultReader};
        this.paramSetter = new JDBCParameterSetter[]{paramSetter};
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
        return autoIncrement;
    }

    public Object getColumnValue(int index, Object value) {
        if (index != 0) {
            throw MESSAGES.simpleTypeRequiresOneIndex();
        }
        return mapper == null ? value : mapper.toColumnValue(value);
    }

    public Object setColumnValue(int index, Object value, Object columnValue) {
        if (index != 0) {
            throw MESSAGES.simpleTypeRequiresOneIndex();
        }
        return mapper == null ? columnValue : mapper.toFieldValue(columnValue);
    }

    public boolean hasMapper() {
        return mapper != null;
    }

    public boolean isSearchable() {
        int jdbcType = jdbcTypes[0];
        return jdbcType != Types.BINARY &&
                jdbcType != Types.BLOB &&
                jdbcType != Types.CLOB &&
                jdbcType != Types.LONGVARBINARY &&
                jdbcType != Types.LONGVARCHAR &&
                jdbcType != Types.VARBINARY;
    }

    public JDBCResultSetReader[] getResultSetReaders() {
        return resultSetReader;
    }

    public JDBCParameterSetter[] getParameterSetter() {
        return paramSetter;
    }
}
