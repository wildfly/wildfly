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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.jboss.logging.Logger;

/**
 * Implementations of this interface are used to set java.sql.PreparedStatement parameters.
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public interface JDBCParameterSetter {
    /**
     * Sets a parameter of a specific JDBC type.
     *
     * @param ps       the java.sql.PreparedStatement to set parameter on
     * @param index    the index of the parameter
     * @param jdbcType the JDBC type of the parameter as defined by java.sql.Types
     * @param value    parameter value
     * @param log      the logger
     * @throws SQLException
     */
    void set(PreparedStatement ps, int index, int jdbcType, Object value, Logger log) throws SQLException;

    abstract class JDBCAbstractParameterSetter implements JDBCParameterSetter {
        public void set(PreparedStatement ps, int index, int jdbcType, Object value, Logger log)
                throws SQLException {
            if (log.isDebugEnabled()) {
                log.debug("param: " +
                        "i=" + index + ", " +
                        "type=" + JDBCUtil.getJDBCTypeName(jdbcType) + ", " +
                        "value=" + ((value == null) ? "NULL" : value));
            }

            if (value == null) {
                ps.setNull(index, jdbcType);
            } else {
                value = JDBCUtil.coerceToSQLType(jdbcType, value);
                setNotNull(ps, index, jdbcType, value, log);
            }
        }

        protected abstract void setNotNull(PreparedStatement ps, int index, int jdbcType, Object value, Logger log)
                throws SQLException;
    }

    /**
     * Types.CLOB, Types.LONGVARCHAR.
     */
    JDBCParameterSetter CLOB = new JDBCAbstractParameterSetter() {
        protected void setNotNull(PreparedStatement ps, int index, int jdbcType, Object value, Logger log)
                throws SQLException {
            String string = value.toString();
            ps.setCharacterStream(index, new StringReader(string), string.length());
        }
    };

    /**
     * Types.BINARY, Types.VARBINARY.
     */
    JDBCParameterSetter BINARY = new JDBCAbstractParameterSetter() {
        protected void setNotNull(PreparedStatement ps, int index, int jdbcType, Object value, Logger log)
                throws SQLException {
            byte[] bytes = JDBCUtil.convertObjectToByteArray(value);
            ps.setBytes(index, bytes);
        }
    };

    /**
     * Types.BLOB, Types.LONGVARBINARY.
     */
    JDBCParameterSetter BLOB = new JDBCAbstractParameterSetter() {
        protected void setNotNull(PreparedStatement ps, int index, int jdbcType, Object value, Logger log)
                throws SQLException {
            byte[] bytes = JDBCUtil.convertObjectToByteArray(value);
            ps.setBinaryStream(index, new ByteArrayInputStream(bytes), bytes.length);
        }
    };

    /**
     * Types.DECIMAL, Types.NUMERIC
     */
    JDBCParameterSetter NUMERIC = new JDBCAbstractParameterSetter() {
        protected void setNotNull(PreparedStatement ps, int index, int jdbcType, Object value, Logger log)
                throws SQLException {
            if (value instanceof BigDecimal) {
                ps.setBigDecimal(index, (BigDecimal) value);
            } else {
                ps.setObject(index, value, jdbcType, 0);
            }
        }
    };

    /**
     * Types.JAVA_OBJECT, Types.OTHER, Types.STRUCT
     */
    JDBCParameterSetter OBJECT = new JDBCAbstractParameterSetter() {
        protected void setNotNull(PreparedStatement ps, int index, int jdbcType, Object value, Logger log)
                throws SQLException {
            ps.setObject(index, value, jdbcType);
        }
    };
}
