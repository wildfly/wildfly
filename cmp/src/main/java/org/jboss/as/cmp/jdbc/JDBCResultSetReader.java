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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import javax.ejb.Handle;
import org.jboss.logging.Logger;

/**
 * Implementations of this interface are used to read java.sql.ResultSet.
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public interface JDBCResultSetReader {
    /**
     * Reads one column from the java.sql.ResultSet.
     *
     * @param rs          the java.sql.ResultSet to read from
     * @param index       the index of the column
     * @param destination the expected Java class of result
     * @param log         the logger
     * @return column value
     * @throws SQLException
     */
    Object get(ResultSet rs, int index, Class<?> destination, Logger log) throws SQLException;

    JDBCResultSetReader CLOB_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            Clob clob = rs.getClob(index);

            String content;
            if (clob == null) {
                content = null;
            } else {
                final Reader reader = clob.getCharacterStream();
                if (reader != null) {
                    int intLength = (int) clob.length();

                    char[] chars;
                    try {
                        if (intLength <= 8192) {
                            chars = new char[intLength];
                            reader.read(chars);
                            content = String.valueOf(chars);
                        } else {
                            StringBuffer buf = new StringBuffer(intLength);
                            chars = new char[8192];
                            int i = reader.read(chars);
                            while (i > 0) {
                                buf.append(chars, 0, i);
                                i = reader.read(chars);
                            }
                            content = buf.toString();
                        }
                    } catch (IOException e) {
                        throw new SQLException("Failed to read CLOB character stream: " + e.getMessage());
                    } finally {
                        JDBCUtil.safeClose(reader);
                    }
                } else {
                    content = null;
                }
            }

            return content;
        }
    };

    JDBCResultSetReader LONGVARCHAR_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            return JDBCUtil.getLongString(rs, index);
        }
    };

    JDBCResultSetReader BINARY_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            Object value = null;
            byte[] bytes = rs.getBytes(index);
            if (!rs.wasNull()) {
                if (destination == byte[].class)
                    value = bytes;
                else
                    value = JDBCUtil.convertToObject(bytes);
            }
            return value;
        }
    };

    JDBCResultSetReader VARBINARY_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            Object value = null;
            byte[] bytes = rs.getBytes(index);
            if (!rs.wasNull()) {
                if (destination == byte[].class)
                    value = bytes;
                else
                    value = JDBCUtil.convertToObject(bytes);
            }
            return value;
        }
    };

    JDBCResultSetReader BLOB_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            Blob blob = rs.getBlob(index);

            Object value;
            if (blob == null) {
                value = null;
            } else {
                InputStream binaryData = blob.getBinaryStream();
                if (binaryData != null) {
                    if (destination == byte[].class)
                        value = JDBCUtil.getByteArray(binaryData);
                    else
                        value = JDBCUtil.convertToObject(binaryData);
                } else {
                    value = null;
                }
            }

            return value;
        }
    };

    JDBCResultSetReader LONGVARBINARY_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            Object value = null;
            InputStream binaryData = rs.getBinaryStream(index);
            if (binaryData != null && !rs.wasNull()) {
                if (destination == byte[].class)
                    value = JDBCUtil.getByteArray(binaryData);
                else
                    value = JDBCUtil.convertToObject(binaryData);
            }
            return value;
        }
    };

    JDBCResultSetReader JAVA_OBJECT_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            return rs.getObject(index);
        }
    };

    JDBCResultSetReader STRUCT_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            return rs.getObject(index);
        }
    };

    JDBCResultSetReader ARRAY_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            return rs.getObject(index);
        }
    };

    JDBCResultSetReader OTHER_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            return rs.getObject(index);
        }
    };

    JDBCResultSetReader JAVA_UTIL_DATE_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            return rs.getTimestamp(index);
        }

        protected Object coerceToJavaType(Object value, Class<?> destination) {
            // make new copy as sub types have problems in comparisons
            java.util.Date result;
            // handle timestamp special because it hoses the millisecond values
            if (value instanceof java.sql.Timestamp) {
                java.sql.Timestamp ts = (java.sql.Timestamp) value;
                // Timestamp returns whole seconds from getTime and partial
                // seconds are retrieved from getNanos()
                // Adrian Brock: Not in 1.4 it doesn't
                long temp = ts.getTime();
                if (temp % 1000 == 0)
                    temp += ts.getNanos() / 1000000;
                result = new java.util.Date(temp);
            } else {
                result = new java.util.Date(((java.util.Date) value).getTime());
            }
            return result;
        }
    };

    JDBCResultSetReader JAVA_SQL_DATE_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            return rs.getDate(index);
        }

        protected Object coerceToJavaType(Object value, Class<?> destination) {
            // make a new copy object; you never know what a driver will return
            return new java.sql.Date(((java.sql.Date) value).getTime());
        }
    };

    JDBCResultSetReader JAVA_SQL_TIME_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            return rs.getTime(index);
        }

        protected Object coerceToJavaType(Object value, Class<?> destination) {
            // make a new copy object; you never know what a driver will return
            return new java.sql.Time(((java.sql.Time) value).getTime());
        }
    };

    JDBCResultSetReader JAVA_SQL_TIMESTAMP_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            return rs.getTimestamp(index);
        }

        protected Object coerceToJavaType(Object value, Class<?> destination) {
            // make a new copy object; you never know what a driver will return
            java.sql.Timestamp orignal = (java.sql.Timestamp) value;
            java.sql.Timestamp copy = new java.sql.Timestamp(orignal.getTime());
            copy.setNanos(orignal.getNanos());
            return copy;
        }
    };

    JDBCResultSetReader BIGDECIMAL_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return rs.getBigDecimal(index);
        }
    };

    JDBCResultSetReader REF_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return rs.getRef(index);
        }
    };

    JDBCResultSetReader BYTE_ARRAY_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return rs.getBytes(index);
        }
    };

    JDBCResultSetReader OBJECT_READER = new AbstractResultSetReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination) throws SQLException {
            return rs.getObject(index);
        }
    };

    JDBCResultSetReader STRING_READER = new JDBCResultSetReader() {
        public Object get(ResultSet rs, int index, Class<?> destination, Logger log) throws SQLException {
            final String result = rs.getString(index);

            if (log.isTraceEnabled()) {
                log.trace("result: i=" + index + ", type=" + destination.getName() + ", value=" + result);
            }

            return result;
        }
    };

    abstract class AbstractPrimitiveReader
            extends AbstractResultSetReader {
        // ResultSetReader implementation

        public Object get(ResultSet rs, int index, Class<?> destination, Logger log)
                throws SQLException {
            Object result = readResult(rs, index, destination);
            if (rs.wasNull())
                result = null;
            else
                result = coerceToJavaType(result, destination);

            if (log.isTraceEnabled()) {
                log.trace("result: i=" + index + ", type=" + destination.getName() + ", value=" + result);
            }

            return result;
        }

        // Protected

        protected Object coerceToJavaType(Object value, Class<?> destination)
                throws SQLException {
            return value;
        }
    }

    JDBCResultSetReader BOOLEAN_READER = new AbstractPrimitiveReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return (rs.getBoolean(index) ? Boolean.TRUE : Boolean.FALSE);
        }
    };

    JDBCResultSetReader BYTE_READER = new AbstractPrimitiveReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return new Byte(rs.getByte(index));
        }
    };

    JDBCResultSetReader CHARACTER_READER = new AbstractPrimitiveReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return rs.getString(index);
        }

        protected Object coerceToJavaType(Object value, Class<?> destination) {
            //
            // java.lang.String --> java.lang.Character or char
            //
            // just grab first character
            if (value instanceof String && (destination == Character.class || destination == Character.TYPE)) {
                return new Character(((String) value).charAt(0));
            } else {
                return value;
            }
        }
    };

    JDBCResultSetReader SHORT_READER = new AbstractPrimitiveReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return new Short(rs.getShort(index));
        }
    };

    JDBCResultSetReader INT_READER = new AbstractPrimitiveReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return new Integer(rs.getInt(index));
        }
    };

    JDBCResultSetReader LONG_READER = new AbstractPrimitiveReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return new Long(rs.getLong(index));
        }
    };

    JDBCResultSetReader FLOAT_READER = new AbstractPrimitiveReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return new Float(rs.getFloat(index));
        }
    };

    JDBCResultSetReader DOUBLE_READER = new AbstractPrimitiveReader() {
        protected Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException {
            return new Double(rs.getDouble(index));
        }
    };

    abstract class AbstractResultSetReader implements JDBCResultSetReader {
        public Object get(ResultSet rs, int index, Class<?> destination, Logger log) throws SQLException {
            Object result = readResult(rs, index, destination);
            if (result != null)
                result = coerceToJavaType(result, destination);

            if (log.isTraceEnabled()) {
                log.trace("result: i=" + index + ", type=" + destination.getName() + ", value=" + result);
            }

            return result;
        }

        protected abstract Object readResult(ResultSet rs, int index, Class<?> destination)
                throws SQLException;

        protected Object coerceToJavaType(Object value, Class<?> destination)
                throws SQLException {
            try {
                //
                // java.rmi.MarshalledObject
                //
                // get unmarshalled value
                if (value instanceof MarshalledObject && !destination.equals(MarshalledObject.class)) {
                    value = ((MarshalledObject) value).get();
                }

                //
                // javax.ejb.Handle
                //
                // get the object back from the handle
                if (value instanceof Handle) {
                    value = ((Handle) value).getEJBObject();
                }

                // Did we get the desired result?
                if (destination.isAssignableFrom(value.getClass())) {
                    return value;
                }

                if (destination == java.math.BigInteger.class && value.getClass() == java.math.BigDecimal.class) {
                    return ((java.math.BigDecimal) value).toBigInteger();
                }

                // oops got the wrong type - nothing we can do
                String className = null;
                Object interfaces = null;
                ClassLoader cl = null;
                if (value != null) {
                    Class<?> valueClass = value.getClass();
                    className = valueClass.getName();
                    interfaces = Arrays.asList(valueClass.getInterfaces());
                    cl = valueClass.getClassLoader();
                }
                throw new SQLException("Got a " + className + "[cl=" + cl +
                        " + interfaces=" + interfaces + ", value=" + value + "] while looking for a " +
                        destination.getName() + "[cl=" + destination.getClassLoader() + "]");
            } catch (RemoteException e) {
                throw new SQLException("Unable to load EJBObject back from Handle: " + e);
            } catch (IOException e) {
                throw new SQLException("Unable to load to deserialize result: " + e);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Unable to load to deserialize result: " + e);
            }
        }
    }
}
