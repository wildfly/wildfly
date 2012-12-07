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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.cmp.CmpConfig;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;

/**
 * @author John Bailey
 */
public class ParsedEntity {
    String dataSourceName;
    String dataSourceMappingName;
    String entityName;
    String tableName;
    Boolean createTable;
    Boolean createTableIfNotExistsSupported;
    Boolean removeTable;
    Boolean alterTable;
    List<String> tablePostCreateCmd = new ArrayList<String>();
    Boolean rowLocking;
    Boolean readOnly;
    Integer readTimeOut;
    Boolean primaryKeyConstraint;
    Map<String, List<String>> loadGroups = new HashMap<String, List<String>>();
    String eagerLoadGroup;
    ParsedReadAhead readAhead;
    Boolean cleanReadAheadOnLoad;
    Integer listCacheMax;
    Integer fetchSize;
    JDBCEntityCommandMetaData entityCommand;
    ParsedOptimisticLocking optimisticLocking;
    ParsedAudit audit;
    Class<?> qlCompiler;
    Boolean throwRuntimeExceptions;
    ParsedCmpField upkField;
    final List<ParsedQuery> queries = new ArrayList<ParsedQuery>();
    final List<ParsedCmpField> cmpFields = new ArrayList<ParsedCmpField>();
    final List<String> lazyLoadGroups = new ArrayList<String>();
    String preferredMappingStyle;
    CmpConfig cmpConfig = new CmpConfig();

    public String getDataSourceName() {
        return dataSourceName;
    }

    public String getDataSourceMappingName() {
        return dataSourceMappingName;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getTableName() {
        return tableName;
    }

    public Boolean getCreateTable() {
        return createTable;
    }

    public Boolean getCreateTableIfNotExistsSupported() {
        return createTableIfNotExistsSupported;
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

    public Boolean getReadOnly() {
        return readOnly;
    }

    public Integer getReadTimeOut() {
        return readTimeOut;
    }

    public Boolean getPrimaryKeyConstraint() {
        return primaryKeyConstraint;
    }

    public Map<String, List<String>> getLoadGroups() {
        return loadGroups;
    }

    public String getEagerLoadGroup() {
        return eagerLoadGroup;
    }

    public ParsedReadAhead getReadAhead() {
        return readAhead;
    }

    public Boolean getCleanReadAheadOnLoad() {
        return cleanReadAheadOnLoad;
    }

    public Integer getListCacheMax() {
        return listCacheMax;
    }

    public Integer getFetchSize() {
        return fetchSize;
    }

    public JDBCEntityCommandMetaData getEntityCommand() {
        return entityCommand;
    }

    public ParsedOptimisticLocking getOptimisticLocking() {
        return optimisticLocking;
    }

    public ParsedAudit getAudit() {
        return audit;
    }

    public Class<?> getQlCompiler() {
        return qlCompiler;
    }

    public Boolean getThrowRuntimeExceptions() {
        return throwRuntimeExceptions;
    }

    public ParsedCmpField getUpkField() {
        return upkField;
    }

    public List<ParsedQuery> getQueries() {
        return queries;
    }

    public List<ParsedCmpField> getCmpFields() {
        return cmpFields;
    }

    public List<String> getLazyLoadGroups() {
        return lazyLoadGroups;
    }

    public String getPreferredMappingStyle() {
        return preferredMappingStyle;
    }

    public CmpConfig getCmpConfig() {
        return cmpConfig;
    }
}
