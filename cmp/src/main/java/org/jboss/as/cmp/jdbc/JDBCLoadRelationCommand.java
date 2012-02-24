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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ejb.EJBException;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCFunctionMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCReadAheadMetaData;
import org.jboss.logging.Logger;

/**
 * Loads relations for a particular entity from a relation table.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCLoadRelationCommand {
    private final JDBCStoreManager manager;
    private final JDBCEntityBridge entity;
    private final Logger log;

    public JDBCLoadRelationCommand(JDBCStoreManager manager) {
        this.manager = manager;
        this.entity = (JDBCEntityBridge) manager.getEntityBridge();

        // Create the Log
        log = Logger.getLogger(
                this.getClass().getName() +
                        "." +
                        manager.getMetaData().getName());
    }

    public Collection execute(JDBCCMRFieldBridge cmrField, Object pk) {
        JDBCCMRFieldBridge relatedCMRField = (JDBCCMRFieldBridge) cmrField.getRelatedCMRField();

        // get the read ahead caches
        ReadAheadCache readAheadCache = manager.getReadAheadCache();
        ReadAheadCache relatedReadAheadCache = cmrField.getRelatedManager().getReadAheadCache();

        // get the finder results associated with this context, if it exists
        ReadAheadCache.EntityReadAheadInfo info = readAheadCache.getEntityReadAheadInfo(pk);
        List loadKeys = info.getLoadKeys();

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // generate the sql
            boolean[] preloadMask = getPreloadMask(cmrField);
            String sql = getSQL(cmrField, preloadMask, loadKeys.size());

            // create the statement
            if (log.isDebugEnabled())
                log.debug("load relation SQL: " + sql);

            // get the connection
            con = cmrField.getDataSource().getConnection();
            ps = con.prepareStatement(sql.toString());

            // Set the fetch size of the statement
            if (entity.getFetchSize() > 0) {
                ps.setFetchSize(entity.getFetchSize());
            }

            // get the load fields
            JDBCCMPFieldBridge[] myKeyFields = getMyKeyFields(cmrField);
            JDBCCMPFieldBridge[] relatedKeyFields = getRelatedKeyFields(cmrField);

            // set the parameters
            int paramIndex = 1;
            for (int i = 0; i < loadKeys.size(); i++) {
                Object key = loadKeys.get(i);
                for (int j = 0; j < myKeyFields.length; ++j)
                    paramIndex = myKeyFields[j].setPrimaryKeyParameters(ps, paramIndex, key);
            }

            // execute statement
            rs = ps.executeQuery();

            // initialize the results map
            Map resultsMap = new HashMap(loadKeys.size());
            for (int i = 0; i < loadKeys.size(); ++i) {
                resultsMap.put(loadKeys.get(i), new ArrayList());
            }

            // load the results
            Object[] ref = new Object[1];
            while (rs.next()) {
                // reset the column index for this row
                int index = 1;

                // ref must be reset to null before each load
                ref[0] = null;

                // if we are loading more then one entity, load the pk from the row
                Object loadedPk = pk;
                if (loadKeys.size() > 1) {
                    // load the pk
                    for (int i = 0; i < myKeyFields.length; ++i) {
                        index = myKeyFields[i].loadPrimaryKeyResults(rs, index, ref);
                        if (ref[0] == null) {
                            break;
                        }
                    }
                    loadedPk = ref[0];
                }

                // load the fk
                ref[0] = null;
                for (int i = 0; i < relatedKeyFields.length; ++i) {
                    index = relatedKeyFields[i].loadPrimaryKeyResults(rs, index, ref);
                    if (ref[0] == null) {
                        break;
                    }
                }
                Object loadedFk = ref[0];

                if (loadedFk != null) {
                    // add this value to the list for loadedPk
                    List results = (List) resultsMap.get(loadedPk);
                    results.add(loadedFk);

                    // if the related cmr field is single valued we can pre-load
                    // the reverse relationship
                    if (relatedCMRField.isSingleValued()) {
                        relatedReadAheadCache.addPreloadData(
                                loadedFk,
                                relatedCMRField,
                                Collections.singletonList(loadedPk));
                    }

                    // read the preload fields
                    if (preloadMask != null) {
                        JDBCFieldBridge[] relatedFields = cmrField.getRelatedJDBCEntity().getTableFields();
                        for (int i = 0; i < relatedFields.length; ++i) {
                            if (preloadMask[i]) {
                                JDBCFieldBridge field = relatedFields[i];
                                ref[0] = null;

                                // read the value and store it in the readahead cache
                                index = field.loadArgumentResults(rs, index, ref);
                                relatedReadAheadCache.addPreloadData(loadedFk, field, ref[0]);
                            }
                        }
                    }
                }
            }

            // set all of the preloaded values
            JDBCReadAheadMetaData readAhead = relatedCMRField.getReadAhead();
            for (Iterator iter = resultsMap.keySet().iterator(); iter.hasNext(); ) {
                Object key = iter.next();

                // get the results for this key
                List results = (List) resultsMap.get(key);

                // store the results list for readahead on-load
                relatedReadAheadCache.addFinderResults(results, readAhead);

                // store the preloaded relationship (unless this is the result we
                // are actually after)
                if (!key.equals(pk)) {
                    readAheadCache.addPreloadData(key, cmrField, results);
                }
            }

            // success, return the results
            return (List) resultsMap.get(pk);
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw MESSAGES.loadRelationFailed(e);
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);
        }
    }

    private String getSQL(JDBCCMRFieldBridge cmrField, boolean[] preloadMask, int keyCount) {
        JDBCCMPFieldBridge[] myKeyFields = getMyKeyFields(cmrField);
        JDBCCMPFieldBridge[] relatedKeyFields = getRelatedKeyFields(cmrField);
        String relationTable = getQualifiedRelationTable(cmrField);
        JDBCEntityBridge relatedEntity = cmrField.getRelatedJDBCEntity();
        String relatedTable = relatedEntity.getQualifiedTableName();

        // do we need to join the relation table and the related table
        boolean join = ((preloadMask != null) || cmrField.allFkFieldsMappedToPkFields())
                && (relatedKeyFields != relatedEntity.getPrimaryKeyFields());

        // aliases for the tables, only required if we are joining the tables
        String relationTableAlias;
        String relatedTableAlias;
        if (join) {
            relationTableAlias = getRelationTable(cmrField);
            relatedTableAlias = (
                    relatedTable.equals(relationTable) ? getRelationTable(cmrField) + '_' + cmrField.getFieldName() : relatedEntity.getTableName()
            );
        } else {
            relationTableAlias = "";
            relatedTableAlias = "";
        }

        JDBCFunctionMappingMetaData selectTemplate = getSelectTemplate(cmrField);
        return selectTemplate == null ?
                getPlainSQL(
                        keyCount,
                        myKeyFields,
                        relationTableAlias,
                        relatedKeyFields,
                        preloadMask,
                        cmrField,
                        relatedTableAlias,
                        relationTable,
                        join,
                        relatedTable)
                :
                getSQLByTemplate(
                        keyCount,
                        myKeyFields,
                        relationTableAlias,
                        relatedKeyFields,
                        preloadMask,
                        cmrField,
                        relatedTableAlias,
                        relationTable,
                        join,
                        relatedTable,
                        selectTemplate);
    }

    private JDBCCMPFieldBridge[] getMyKeyFields(JDBCCMRFieldBridge cmrField) {
        if (cmrField.getRelationMetaData().isTableMappingStyle()) {
            // relation table
            return (JDBCCMPFieldBridge[]) cmrField.getTableKeyFields();
        } else if (cmrField.getRelatedCMRField().hasForeignKey()) {
            // related has foreign key
            return (JDBCCMPFieldBridge[]) cmrField.getRelatedCMRField().getForeignKeyFields();
        } else {
            // i have foreign key
            return (JDBCCMPFieldBridge[]) entity.getPrimaryKeyFields();
        }
    }

    private static JDBCCMPFieldBridge[] getRelatedKeyFields(JDBCCMRFieldBridge cmrField) {
        if (cmrField.getRelationMetaData().isTableMappingStyle()) {
            // relation table
            return (JDBCCMPFieldBridge[]) cmrField.getRelatedCMRField().getTableKeyFields();
        } else if (cmrField.getRelatedCMRField().hasForeignKey()) {
            // related has foreign key
            return (JDBCCMPFieldBridge[]) cmrField.getRelatedJDBCEntity().getPrimaryKeyFields();
        } else {
            // i have foreign key
            return (JDBCCMPFieldBridge[]) cmrField.getForeignKeyFields();
        }
    }

    private static boolean[] getPreloadMask(JDBCCMRFieldBridge cmrField) {
        boolean[] preloadMask = null;
        if (cmrField.getReadAhead().isOnFind()) {
            JDBCEntityBridge relatedEntity = cmrField.getRelatedJDBCEntity();
            String eagerLoadGroup = cmrField.getReadAhead().getEagerLoadGroup();
            preloadMask = relatedEntity.getLoadGroupMask(eagerLoadGroup);
        }
        return preloadMask;
    }

    private String getQualifiedRelationTable(JDBCCMRFieldBridge cmrField) {
        if (cmrField.getRelationMetaData().isTableMappingStyle()) {
            // relation table
            return cmrField.getQualifiedTableName();
        } else if (cmrField.getRelatedCMRField().hasForeignKey()) {
            // related has foreign key
            return cmrField.getRelatedJDBCEntity().getQualifiedTableName();
        } else {
            // i have foreign key
            return entity.getQualifiedTableName();
        }
    }

    private String getRelationTable(JDBCCMRFieldBridge cmrField) {
        if (cmrField.getRelationMetaData().isTableMappingStyle()) {
            // relation table
            return cmrField.getTableName();
        } else if (cmrField.getRelatedCMRField().hasForeignKey()) {
            // related has foreign key
            return cmrField.getRelatedJDBCEntity().getTableName();
        } else {
            // i have foreign key
            return entity.getTableName();
        }
    }

    private JDBCFunctionMappingMetaData getSelectTemplate(JDBCCMRFieldBridge cmrField) {

        JDBCFunctionMappingMetaData selectTemplate = null;
        if (cmrField.getRelationMetaData().isTableMappingStyle()) {
            // relation table
            if (cmrField.getRelationMetaData().hasRowLocking()) {
                selectTemplate =
                        cmrField.getRelationMetaData().getTypeMapping().getRowLockingTemplate();
                if (selectTemplate == null) {
                    throw CmpMessages.MESSAGES.rowLockingNotAllowed();
                }
            }
        } else if (cmrField.getRelatedCMRField().hasForeignKey()) {
            // related has foreign key
            if (cmrField.getRelatedJDBCEntity().getMetaData().hasRowLocking()) {
                selectTemplate =
                        cmrField.getRelatedJDBCEntity().getMetaData().getTypeMapping().getRowLockingTemplate();
                if (selectTemplate == null) {
                    throw CmpMessages.MESSAGES.rowLockingNotAllowed();
                }
            }
        } else {
            // i have foreign key
            if (entity.getMetaData().hasRowLocking()) {
                selectTemplate = entity.getMetaData().getTypeMapping().getRowLockingTemplate();
                if (selectTemplate == null) {
                    throw CmpMessages.MESSAGES.rowLockingNotAllowed();
                }
            }
        }
        return selectTemplate;
    }

    private static String getPlainSQL(int keyCount,
                                      JDBCCMPFieldBridge[] myKeyFields,
                                      String relationTableAlias,
                                      JDBCCMPFieldBridge[] relatedKeyFields,
                                      boolean[] preloadMask,
                                      JDBCCMRFieldBridge cmrField,
                                      String relatedTableAlias,
                                      String relationTable,
                                      boolean join,
                                      String relatedTable) {
        //
        // column names clause
        //
        StringBuffer sql = new StringBuffer(400);
        sql.append(SQLUtil.SELECT);

        if (keyCount > 1) {
            SQLUtil.getColumnNamesClause(myKeyFields, relationTableAlias, sql)
                    .append(SQLUtil.COMMA);
        }
        SQLUtil.getColumnNamesClause(relatedKeyFields, relationTableAlias, sql);

        if (preloadMask != null) {
            SQLUtil.appendColumnNamesClause(
                    cmrField.getRelatedJDBCEntity().getTableFields(),
                    preloadMask,
                    relatedTableAlias,
                    sql);
        }

        //
        // from clause
        //
        sql.append(SQLUtil.FROM).append(relationTable);
        if (join) {
            sql.append(' ')
                    .append(relationTableAlias)
                    .append(SQLUtil.COMMA)
                    .append(relatedTable)
                    .append(' ')
                    .append(relatedTableAlias);
        }

        //
        // where clause
        //
        sql.append(SQLUtil.WHERE);
        // add the join
        if (join) {
            // join the tables
            sql.append('(');
            SQLUtil.getJoinClause(
                    relatedKeyFields,
                    relationTableAlias,
                    cmrField.getRelatedJDBCEntity().getPrimaryKeyFields(),
                    relatedTableAlias,
                    sql)
                    .append(')')
                    .append(SQLUtil.AND)
                    .append('(');
        }

        // add the keys
        String pkWhere = SQLUtil.getWhereClause(myKeyFields, relationTableAlias, new StringBuffer(50)).toString();
        for (int i = 0; i < keyCount; i++) {
            if (i > 0)
                sql.append(SQLUtil.OR);
            sql.append('(').append(pkWhere).append(')');
        }

        if (join)
            sql.append(')');

        return sql.toString();
    }

    private static String getSQLByTemplate(int keyCount,
                                           JDBCCMPFieldBridge[] myKeyFields,
                                           String relationTableAlias,
                                           JDBCCMPFieldBridge[] relatedKeyFields,
                                           boolean[] preloadMask,
                                           JDBCCMRFieldBridge cmrField,
                                           String relatedTableAlias,
                                           String relationTable,
                                           boolean join,
                                           String relatedTable,
                                           JDBCFunctionMappingMetaData selectTemplate) {
        //
        // column names clause
        //
        StringBuffer columnNamesClause = new StringBuffer(100);
        if (keyCount > 1) {
            SQLUtil.getColumnNamesClause(myKeyFields, relationTableAlias, columnNamesClause)
                    .append(SQLUtil.COMMA);
        }
        SQLUtil.getColumnNamesClause(relatedKeyFields, relationTableAlias, columnNamesClause);
        if (preloadMask != null) {
            SQLUtil.appendColumnNamesClause(
                    cmrField.getRelatedJDBCEntity().getTableFields(),
                    preloadMask,
                    relatedTableAlias,
                    columnNamesClause);
        }

        //
        // from clause
        //
        StringBuffer fromClause = new StringBuffer(100);
        fromClause.append(relationTable);
        if (join) {
            fromClause.append(' ')
                    .append(relationTableAlias)
                    .append(SQLUtil.COMMA)
                    .append(relatedTable)
                    .append(' ')
                    .append(relatedTableAlias);
        }

        //
        // where clause
        //
        StringBuffer whereClause = new StringBuffer(150);
        // add the join
        if (join) {
            // join the tables
            whereClause.append('(');
            SQLUtil.getJoinClause(
                    relatedKeyFields,
                    relationTableAlias,
                    cmrField.getRelatedJDBCEntity().getPrimaryKeyFields(),
                    relatedTableAlias,
                    whereClause)
                    .append(')')
                    .append(SQLUtil.AND)
                    .append('(');
        }

        // add the keys
        String pkWhere = SQLUtil.getWhereClause(myKeyFields, relationTableAlias, new StringBuffer(50)).toString();
        for (int i = 0; i < keyCount; i++) {
            if (i > 0) {
                whereClause.append(SQLUtil.OR);
            }
            whereClause.append('(').append(pkWhere).append(')');
        }

        if (join) {
            whereClause.append(')');
        }

        //
        // assemble pieces into final statement
        //
        String[] args = new String[]{
                columnNamesClause.toString(),
                fromClause.toString(),
                whereClause.toString(),
                null // order by
        };
        return selectTemplate.getFunctionSql(args, new StringBuffer(500)).toString();
    }
}
