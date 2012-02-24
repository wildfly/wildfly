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
package org.jboss.as.cmp.jdbc2;

import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMPFieldBridge2;
import org.jboss.as.cmp.jdbc.metadata.JDBCDeclaredQueryMetaData;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.jdbc.QueryParameter;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class DeclaredSQLQueryCommand extends AbstractQueryCommand {
    private JDBCCMPFieldBridge2 selectedField;

    public DeclaredSQLQueryCommand(JDBCEntityBridge2 entity, JDBCDeclaredQueryMetaData metadata) {
        initResultReader(entity, metadata);

        this.sql = buildSQL(metadata);
        this.sql = parseParameters(this.sql, metadata);

        setResultType(metadata.getMethod().getReturnType());

        log =
                Logger.getLogger(getClass().getName() + "." + entity.getEntityName() + "#" + metadata.getMethod().getName());
        log.debug("sql: " + sql);
    }

    private void initResultReader(JDBCEntityBridge2 entity, JDBCDeclaredQueryMetaData metadata) {
        String entityName = metadata.getEJBName();
        if (entityName != null) {
            Catalog catalog = entity.getManager().getCatalog();
            JDBCEntityBridge2 otherEntity = (JDBCEntityBridge2) catalog.getEntityByEJBName(entityName);
            if (otherEntity == null) {
                throw MESSAGES.unknownEntity(entityName);
            }
            this.entity = otherEntity;
        } else {
            this.entity = entity;
        }

        String fieldName = metadata.getFieldName();
        if (fieldName == null) {
            setEntityReader(this.entity, metadata.isSelectDistinct());
        } else {
            selectedField = (JDBCCMPFieldBridge2) entity.getFieldByName(fieldName);
            if (selectedField == null) {
                throw MESSAGES.unknownCmpField(fieldName);
            }

            setFieldReader(selectedField);
        }
    }

    private String buildSQL(JDBCDeclaredQueryMetaData metadata) {
        StringBuffer sql = new StringBuffer(300);

        sql.append(SQLUtil.SELECT);
        if (metadata.isSelectDistinct()) {
            sql.append(SQLUtil.DISTINCT);
        }

        String alias = metadata.getAlias();
        String from = metadata.getFrom();
        String table;
        String selectList;
        if (metadata.getFieldName() == null) {
            // we are selecting a full entity
            table = this.entity.getQualifiedTableName();

            // get a list of all fields to be loaded
            // put pk fields in front
            String tableAlias = getTableAlias(alias, from, this.entity.getTableName());
            selectList = SQLUtil.getColumnNamesClause(this.entity.getPrimaryKeyFields(),
                    tableAlias,
                    new StringBuffer(35)).toString();
        } else {
            // we are just selecting one field
            JDBCStoreManager2 manager = (JDBCStoreManager2) selectedField.getManager();
            table = manager.getEntityBridge().getQualifiedTableName();
            selectList = SQLUtil.getColumnNamesClause(selectedField,
                    getTableAlias(alias, from, manager.getEntityBridge().getTableName()),
                    new StringBuffer()).toString();
        }
        sql.append(selectList);
        String additionalColumns = metadata.getAdditionalColumns();
        if (additionalColumns != null) {
            sql.append(additionalColumns);
        }
        sql.append(SQLUtil.FROM).append(table);
        if (alias != null) {
            sql.append(' ').append(alias);
        }
        if (from != null) {
            sql.append(' ').append(from);
        }

        String where = metadata.getWhere();
        if (where != null && where.trim().length() > 0) {
            sql.append(SQLUtil.WHERE).append(where);
        }

        String order = metadata.getOrder();
        if (order != null && order.trim().length() > 0) {
            sql.append(SQLUtil.ORDERBY).append(order);
        }

        String other = metadata.getOther();
        if (other != null && other.trim().length() > 0) {
            sql.append(' ').append(other);
        }
        return sql.toString();
    }

    private static String getTableAlias(String alias, String from, String table) {
        String tableAlias;
        if (alias != null) {
            tableAlias = alias;
        } else if (from != null) {
            tableAlias = table;
        } else {
            tableAlias = SQLUtil.EMPTY_STRING;
        }
        return tableAlias;
    }

    /**
     * Replaces the parameters in the specific sql with question marks, and
     * initializes the parameter setting code. Parameters are encoded in curly
     * brackets use a zero based index.
     *
     * @param sql the sql statement that is parsed for parameters
     * @return the original sql statement with the parameters replaced with a
     *         question mark
     */
    protected String parseParameters(String sql, JDBCDeclaredQueryMetaData metadata) {
        StringBuffer sqlBuf = new StringBuffer();
        ArrayList params = new ArrayList();

        // Replace placeholders {0} with ?
        if (sql != null) {
            sql = sql.trim();

            StringTokenizer tokens = new StringTokenizer(sql, "{}", true);
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                if (token.equals("{")) {
                    token = tokens.nextToken();
                    if (Character.isDigit(token.charAt(0))) {
                        QueryParameter parameter = new QueryParameter(entity.getManager(), metadata.getMethod(), token);

                        // of if we are here we can assume that we have
                        // a parameter and not a function
                        sqlBuf.append("?");
                        params.add(parameter);

                        if (!tokens.nextToken().equals("}")) {
                            throw MESSAGES.missingClosingCurlyBrace(sql);
                        }
                    } else {
                        // ok we don't have a parameter, we have a function
                        // push the tokens on the buffer and continue
                        sqlBuf.append("{").append(token);
                    }
                } else {
                    // not parameter... just append it
                    sqlBuf.append(token);
                }
            }
        }

        setParameters(params);

        return sqlBuf.toString();
    }
}
