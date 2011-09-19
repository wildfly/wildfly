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

package org.jboss.as.cmp.jdbc.metadata.parser;

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldPropertyMetaData;

/**
 * @author John Bailey
 */
public class ParsedCmpField {
    Class<?> unknownPk = null;
    String fieldName = null;
    Class<?> fieldType = null;
    Boolean readOnly = null;
    Integer readTimeOut = null;
    String columnName = null;
    Integer jdbcType = null;
    String sqlType = null;
    Boolean autoIncrement = null;
    Boolean notNull;
    final List<JDBCCMPFieldPropertyMetaData> propertyOverrides = new ArrayList<JDBCCMPFieldPropertyMetaData>();
    Boolean genIndex = null;
    Boolean checkDirtyAfterGet = null;
    String stateFactory = null;

    public Class<?> getUnknownPk() {
        return unknownPk;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public Integer getReadTimeOut() {
        return readTimeOut;
    }

    public String getColumnName() {
        return columnName;
    }

    public Integer getJdbcType() {
        return jdbcType;
    }

    public String getSqlType() {
        return sqlType;
    }

    public Boolean getAutoIncrement() {
        return autoIncrement;
    }

    public Boolean getNotNull() {
        return notNull;
    }

    public List<JDBCCMPFieldPropertyMetaData> getPropertyOverrides() {
        return propertyOverrides;
    }

    public Boolean getGenIndex() {
        return genIndex;
    }

    public Boolean getCheckDirtyAfterGet() {
        return checkDirtyAfterGet;
    }

    public String getStateFactory() {
        return stateFactory;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }
}
