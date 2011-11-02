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

/**
 * This interface represents a mapping between a Java type and JDBC type.
 * The properties all return arrays, because this type system supports the
 * mapping of java classes to multiple columns.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:loubyansky@ua.fm">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public interface JDBCType {
    String[] getColumnNames();

    Class[] getJavaTypes();

    int[] getJDBCTypes();

    String[] getSQLTypes();

    boolean[] getNotNull();

    boolean[] getAutoIncrement();

    JDBCResultSetReader[] getResultSetReaders();

    JDBCParameterSetter[] getParameterSetter();

    Object getColumnValue(int index, Object value);

    Object setColumnValue(int index, Object value, Object columnValue);

    boolean hasMapper();

    boolean isSearchable();
}
