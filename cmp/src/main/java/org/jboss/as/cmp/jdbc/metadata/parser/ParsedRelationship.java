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
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationMetaData;

/**
 * @author John Bailey
 */
public class ParsedRelationship {
    String relationName;
    JDBCRelationMetaData.MappingStyle mappingStyle;
    String dataSourceName;
    String datasourceMapping;
    String tableName;
    Boolean createTable;
    Boolean removeTable;
    Boolean alterTable;
    final List<String> tablePostCreateCmd = new ArrayList<String>();
    Boolean rowLocking;
    Boolean primaryKeyConstraint;
    Boolean readOnly;
    Integer readTimeOut;
    List<ParsedRelationshipRole> roles = new ArrayList<ParsedRelationshipRole>();

    public String getRelationName() {
        return relationName;
    }

    public JDBCRelationMetaData.MappingStyle getMappingStyle() {
        return mappingStyle;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public String getDatasourceMapping() {
        return datasourceMapping;
    }

    public String getTableName() {
        return tableName;
    }

    public Boolean getCreateTable() {
        return createTable;
    }

    public Boolean getRemoveTable() {
        return removeTable;
    }

    public Boolean getAlterTable() {
        return alterTable;
    }

    public List<String> getTablePostCreateCmd() {
        return tablePostCreateCmd;
    }

    public Boolean getRowLocking() {
        return rowLocking;
    }

    public Boolean getPrimaryKeyConstraint() {
        return primaryKeyConstraint;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public Integer getReadTimeOut() {
        return readTimeOut;
    }

    public List<ParsedRelationshipRole> getRoles() {
        return roles;
    }
}
