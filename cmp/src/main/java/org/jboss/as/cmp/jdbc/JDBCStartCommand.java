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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.sql.DataSource;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.CmpLogger;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.bridge.EntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCFunctionMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationshipRoleMetaData;
import org.jboss.logging.Logger;

/**
 * JDBCStartCommand creates the table if specified in xml.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Oberg</a>
 * @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
 * @author <a href="mailto:shevlandj@kpi.com.au">Joe Shevland</a>
 * @author <a href="mailto:justin@j-m-f.demon.co.uk">Justin Forder</a>
 * @author <a href="mailto:michel.anke@wolmail.nl">Michel de Groot</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @author <a href="mailto:heiko.rupp@cellent.de">Heiko W.Rupp</a>
 * @author <a href="mailto:joachim@cabsoft.be">Joachim Van der Auwera</a>
 * @version $Revision: 81030 $
 */
public final class JDBCStartCommand {
    private static final String IDX_POSTFIX = "_idx";
    private static final Object CREATED_TABLES_KEY = new Object();
    private final JDBCEntityPersistenceStore manager;
    private final JDBCAbstractEntityBridge entity;
    private final JDBCEntityMetaData entityMetaData;
    private final Logger log;
    private int idxCount = 0;

    public JDBCStartCommand(JDBCEntityPersistenceStore manager) {
        this.manager = manager;
        entity = manager.getEntityBridge();
        entityMetaData = manager.getMetaData();

        // Create the Log
        log = Logger.getLogger(this.getClass().getName() +
                "." +
                manager.getMetaData().getName());

        // Start index counter at 1
        idxCount = 1;
    }

    public void execute() {
        boolean tableExisted = SQLUtil.tableExists(entity.getQualifiedTableName(), entity.getDataSource());
        if (tableExisted) {
            manager.addExistingTable(entity.getEntityName());
        }

        if (tableExisted && entityMetaData.getAlterTable()) {
            SQLUtil.OldColumns oldColumns = SQLUtil.getOldColumns(entity.getQualifiedTableName(), entity.getDataSource());
            ArrayList oldNames = oldColumns.getColumnNames();
            ArrayList oldTypes = oldColumns.getTypeNames();
            ArrayList oldSizes = oldColumns.getColumnSizes();
            SQLUtil.OldIndexes oldIndexes = null;
            ArrayList newNames = new ArrayList();
            JDBCFieldBridge[] fields = entity.getTableFields();
            String tableName = entity.getQualifiedTableName();
            for (int i = 0; i < fields.length; i++) {
                JDBCFieldBridge field = fields[i];
                JDBCType jdbcType = field.getJDBCType();
                String[] columnNames = jdbcType.getColumnNames();
                String[] sqlTypes = jdbcType.getSQLTypes();
                boolean[] notNull = jdbcType.getNotNull();

                for (int j = 0; j < columnNames.length; j++) {
                    String name = columnNames[j];
                    String ucName = name.toUpperCase();

                    newNames.add(ucName);

                    int oldIndex = oldNames.indexOf(ucName);
                    if (oldIndex == -1) {
                        // add new column
                        StringBuffer buf = new StringBuffer(sqlTypes[j]);
                        if (notNull[j]) {
                            buf.append(SQLUtil.NOT).append(SQLUtil.NULL);
                        }
                        alterTable(entity.getDataSource(),
                                entityMetaData.getTypeMapping().getAddColumnTemplate(),
                                tableName, name, buf.toString());
                    } else {
                        // alter existing columns
                        // only CHAR and VARCHAR fields are altered, and only when they are longer then before
                        String type = (String) oldTypes.get(oldIndex);
                        if (type.equals("CHAR") || type.equals("VARCHAR")) {
                            try {
                                // get new length
                                String l = sqlTypes[j];
                                l = l.substring(l.indexOf('(') + 1, l.length() - 1);
                                Integer oldLength = (Integer) oldSizes.get(oldIndex);
                                if (Integer.parseInt(l) > oldLength.intValue()) {
                                    alterTable(entity.getDataSource(),
                                            entityMetaData.getTypeMapping().getAlterColumnTemplate(),
                                            tableName, name, sqlTypes[j]);
                                }
                            } catch (Exception e) {
                                CmpLogger.ROOT_LOGGER.exceptionAlterTable(e);
                            }
                        }
                    }
                }

                // see if we have to add an index for the field
                JDBCCMPFieldMetaData fieldMD = entity.getMetaData().getCMPFieldByName(field.getFieldName());
                if (fieldMD != null && fieldMD.isIndexed()) {
                    if (oldIndexes == null) {
                        oldIndexes = SQLUtil.getOldIndexes(entity.getQualifiedTableName(), entity.getDataSource());
                        idxCount = oldIndexes.getIndexNames().size();
                    }
                    if (!hasIndex(oldIndexes, field)) {
                        createCMPIndex(entity.getDataSource(), field, oldIndexes.getIndexNames());
                    }

                }
            } // for  int i;

            // delete old columns
            Iterator it = oldNames.iterator();
            while (it.hasNext()) {
                String name = (String) (it.next());
                if (!newNames.contains(name)) {
                    alterTable(entity.getDataSource(),
                            entityMetaData.getTypeMapping().getDropColumnTemplate(),
                            tableName, name, "");
                }
            }

        }

        // Create table if necessary
        if (entityMetaData.getCreateTable() && !manager.hasCreateTable(entity.getEntityName())) {
            DataSource dataSource = entity.getDataSource();

            createTable(dataSource, entity.getQualifiedTableName(), getEntityCreateTableSQL(dataSource));

            // create indices only if table did not yet exist.
            if (!tableExisted) {
                createCMPIndices(dataSource,
                        SQLUtil.getOldIndexes(entity.getQualifiedTableName(),
                                entity.getDataSource()).getIndexNames());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Indices for table " + entity.getQualifiedTableName() + "not created as table existed");
                }
            }


            // issue extra (user-defined) sql for table
            if (!tableExisted) {
                issuePostCreateSQL(dataSource,
                        entity.getMetaData().getDefaultTablePostCreateCmd(),
                        entity.getQualifiedTableName());
            }

            manager.addCreateTable(entity.getEntityName());
        } else {
            log.debug("Table not create as requested: " + entity.getQualifiedTableName());
        }

        // create relation tables
        JDBCAbstractCMRFieldBridge[] cmrFields = entity.getCMRFields();
        for (int i = 0; i < cmrFields.length; ++i) {
            JDBCAbstractCMRFieldBridge cmrField = cmrFields[i];
            JDBCRelationMetaData relationMetaData = cmrField.getMetaData().getRelationMetaData();
            DataSource dataSource = manager.getDataSource(relationMetaData.getDataSourceName());

            // if the table for the related entity has been created
            final EntityBridge relatedEntity = cmrField.getRelatedEntity();
            if (relationMetaData.isTableMappingStyle() && manager.hasCreateTable(relatedEntity.getEntityName())) {
                boolean relTableExisted = SQLUtil.tableExists(cmrField.getQualifiedTableName(), entity.getDataSource());

                if (relTableExisted) {
                    if (relationMetaData.getAlterTable()) {
                        ArrayList oldNames = SQLUtil.getOldColumns(cmrField.getQualifiedTableName(), dataSource).getColumnNames();
                        ArrayList newNames = new ArrayList();
                        JDBCFieldBridge[] leftKeys = cmrField.getTableKeyFields();
                        JDBCFieldBridge[] rightKeys = cmrField.getRelatedCMRField().getTableKeyFields();
                        JDBCFieldBridge[] fields = new JDBCFieldBridge[leftKeys.length + rightKeys.length];
                        System.arraycopy(leftKeys, 0, fields, 0, leftKeys.length);
                        System.arraycopy(rightKeys, 0, fields, leftKeys.length, rightKeys.length);
                        // have to append field names to leftKeys, rightKeys...

                        boolean different = false;
                        for (int j = 0; j < fields.length; j++) {
                            JDBCFieldBridge field = fields[j];

                            String name = field.getJDBCType().getColumnNames()[0].toUpperCase();
                            newNames.add(name);

                            if (!oldNames.contains(name)) {
                                different = true;
                                break;
                            }
                        } // for int j;

                        if (!different) {
                            Iterator it = oldNames.iterator();
                            while (it.hasNext()) {
                                String name = (String) (it.next());
                                if (!newNames.contains(name)) {
                                    different = true;
                                    break;
                                }
                            }
                        }

                        if (different) {
                            // only log, don't drop table is this can cause data loss
                            CmpLogger.ROOT_LOGGER.incorrectCmrTableStructure(cmrField.getQualifiedTableName());

                            //SQLUtil.dropTable(entity.getDataSource(), cmrField.getQualifiedTableName());
                        }

                    } // if alter-table

                } // if existed

                // create the relation table
                if (relationMetaData.isTableMappingStyle() && !relationMetaData.isTableCreated()) {
                    if (relationMetaData.getCreateTable()) {
                        createTable(dataSource, cmrField.getQualifiedTableName(),
                                getRelationCreateTableSQL(cmrField, dataSource));
                    } else {
                        log.debug("Relation table not created as requested: " + cmrField.getQualifiedTableName());
                    }

                    // create Indices if needed
                    createCMRIndex(dataSource, cmrField);

                    if (relationMetaData.getCreateTable()) {
                        issuePostCreateSQL(dataSource,
                                relationMetaData.getDefaultTablePostCreateCmd(),
                                cmrField.getQualifiedTableName());
                    }
                }
            }
        }
    }

    public void addForeignKeyConstraints() {
        JDBCAbstractCMRFieldBridge[] cmrFields = entity.getCMRFields();
        for (int i = 0; i < cmrFields.length; ++i) {
            JDBCAbstractCMRFieldBridge cmrField = cmrFields[i];
            EntityBridge relatedEntity = cmrField.getRelatedEntity();
            JDBCRelationMetaData relationMetaData = cmrField.getMetaData().getRelationMetaData();

            if (relationMetaData.isForeignKeyMappingStyle() && (manager.hasCreateTable(relatedEntity.getEntityName()))) {
                createCMRIndex(((JDBCAbstractEntityBridge) relatedEntity).getDataSource(), cmrField);
            }

            // Create fk constraint
            addForeignKeyConstraint(cmrField);
        }
    }

    /**
     * Check whether a required index already exists on a table
     *
     * @param oldIndexes list of existing indexes
     * @param field      field we test the existence of an index for
     * @return True if the field has an index; otherwise false
     */
    private boolean hasIndex(SQLUtil.OldIndexes oldIndexes, JDBCFieldBridge field) {
        JDBCType jdbcType = field.getJDBCType();
        String[] columns = jdbcType.getColumnNames();
        ArrayList idxNames = oldIndexes.getIndexNames();
        ArrayList idxColumns = oldIndexes.getColumnNames();

        // check if the columns are in the same index
        String indexName = null;
        for (int i = 0; i < columns.length; ++i) {
            String column = columns[i];
            int index = columnIndex(idxColumns, column);
            if (index == -1) {
                return false;
            }

            if (indexName == null) {
                indexName = (String) idxNames.get(index);
            } else if (!indexName.equals(idxNames.get(index))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check whether a required index already exists on a table
     *
     * @param oldIndexes list of existing indexes
     * @param column     column we test the existence of an index for
     * @return True if the column has an index; otherwise false
     */
    private boolean hasIndex(SQLUtil.OldIndexes oldIndexes, String column) {
        ArrayList idxColumns = oldIndexes.getColumnNames();
        if (columnIndex(idxColumns, column) == -1) {
            return false;
        }
        return true;
    }

    private int columnIndex(ArrayList idxColumns, String column) {
        for (int j = 0; j < idxColumns.size(); ++j) {
            String idxColumn = (String) idxColumns.get(j);
            idxColumn = idxColumn.trim();
            while (idxColumn.startsWith("\"")) {
                idxColumn = idxColumn.substring(1);
            }
            while (idxColumn.endsWith("\"")) {
                idxColumn = idxColumn.substring(0, idxColumn.length() - 1);
            }

            if (idxColumn.equalsIgnoreCase(column)) {
                return j;
            }
        }
        return -1;
    }

    private void alterTable(DataSource dataSource, JDBCFunctionMappingMetaData mapping, String tableName, String fieldName, String fieldStructure) {
        StringBuffer sqlBuf = new StringBuffer();
        mapping.getFunctionSql(new String[]{tableName, fieldName, fieldStructure}, sqlBuf);
        String sql = sqlBuf.toString();

        if (log.isDebugEnabled())
            log.debug("Executing: " + sql);

        // suspend the current transaction
        TransactionManager tm = manager.getComponent().getTransactionManager();
        Transaction oldTransaction;
        try {
            oldTransaction = tm.suspend();
        } catch (Exception e) {
            throw MESSAGES.couldNotSuspendAfterAlterTable(e);
        }

        try {
            Connection con = null;
            Statement statement = null;
            try {
                con = dataSource.getConnection();
                statement = con.createStatement();
                statement.executeUpdate(sql);
            } finally {
                // make sure to close the connection and statement before
                // committing the transaction or XA will break
                JDBCUtil.safeClose(statement);
                JDBCUtil.safeClose(con);
            }
        } catch (Exception e) {
            throw MESSAGES.errorAlteringTable(tableName, sql, e);
        } finally {
            try {
                // resume the old transaction
                if (oldTransaction != null) {
                    tm.resume(oldTransaction);
                }
            } catch (Exception e) {
                throw MESSAGES.couldNotReattachAfterAlterTable(e);
            }
        }

        // success
        if (log.isDebugEnabled())
            log.debug("Table altered successfully.");
    }

    private void createTable(DataSource dataSource, String tableName, String sql) {
        // does this table already exist
        if (SQLUtil.tableExists(tableName, dataSource)) {
            log.debug("Table '" + tableName + "' already exists");
            return;
        }

        // since we use the pools, we have to do this within a transaction

        // suspend the current transaction
        TransactionManager tm = manager.getComponent().getTransactionManager();
        Transaction oldTransaction;
        try {
            oldTransaction = tm.suspend();
        } catch (Exception e) {
            throw MESSAGES.couldNotSuspendBeforeCreateTable(e);
        }

        try {
            Connection con = null;
            Statement statement = null;
            try {
                // execute sql
                if (log.isDebugEnabled()) {
                    log.debug("Executing SQL: " + sql);
                }

                con = dataSource.getConnection();
                statement = con.createStatement();
                statement.executeUpdate(sql);
            } finally {
                // make sure to close the connection and statement before
                // committing the transaction or XA will break
                JDBCUtil.safeClose(statement);
                JDBCUtil.safeClose(con);
            }
        } catch (Exception e) {
            throw MESSAGES.errorCreatingTable(tableName, e);
        } finally {
            try {
                // resume the old transaction
                if (oldTransaction != null) {
                    tm.resume(oldTransaction);
                }
            } catch (Exception e) {
                throw MESSAGES.couldNotReattachAfterCreateTable();
            }
        }

        // success
        manager.addCreateTable(tableName);
    }

    /**
     * Create an index on a field. Does the create
     *
     * @param dataSource
     * @param tableName  In which table is the index?
     * @param indexName  Which is the index?
     * @param sql        The SQL statement to issue
     * @
     */
    private void createIndex(DataSource dataSource, String tableName, String indexName, String sql) {
        // we are only called directly after creating a table
        // since we use the pools, we have to do this within a transaction
        // suspend the current transaction
        TransactionManager tm = manager.getComponent().getTransactionManager();
        Transaction oldTransaction;
        try {
            oldTransaction = tm.suspend();
        } catch (Exception e) {
            throw MESSAGES.couldNotSuspendBeforeCreateIndex(e);
        }

        try {
            Connection con = null;
            Statement statement = null;
            try {
                // execute sql
                if (log.isDebugEnabled()) {
                    log.debug("Executing SQL: " + sql);
                }
                con = dataSource.getConnection();
                statement = con.createStatement();
                statement.executeUpdate(sql);
            } finally {
                // make sure to close the connection and statement before
                // committing the transaction or XA will break
                JDBCUtil.safeClose(statement);
                JDBCUtil.safeClose(con);
            }
        } catch (Exception e) {
            throw MESSAGES.couldNotCreateIndex(indexName, tableName, e);
        } finally {
            try {
                // resume the old transaction
                if (oldTransaction != null) {
                    tm.resume(oldTransaction);
                }
            } catch (Exception e) {
                throw MESSAGES.couldNotReattachAfterCreateIndex(e);
            }
        }
    }


    /**
     * Send (user-defined) SQL commands to the server.
     * The commands can be found in the &lt;sql-statement&gt; elements
     * within the &lt;post-table-create&gt; tag in jbossjdbc-cmp.xml
     *
     * @param dataSource
     */
    private void issuePostCreateSQL(DataSource dataSource, List sql, String table) {
        if (sql == null) { // no work to do.
            log.trace("issuePostCreateSQL: sql is null");
            return;
        }

        log.debug("issuePostCreateSQL::sql: " + sql.toString() + " on table " + table);

        TransactionManager tm = manager.getComponent().getTransactionManager();
        Transaction oldTransaction;

        try {
            oldTransaction = tm.suspend();
        } catch (Exception e) {
            throw MESSAGES.couldNotSuspendBeforeSendingSql(e);
        }

        String currentCmd = "";

        try {
            Connection con = null;
            Statement statement = null;
            try {
                con = dataSource.getConnection();
                statement = con.createStatement();

                // execute sql
                for (int i = 0; i < sql.size(); i++) {
                    currentCmd = (String) sql.get(i);
                    /*
                    * Replace %%t in the sql command with the current table name
                    */
                    currentCmd = replaceTable(currentCmd, table);
                    currentCmd = replaceIndexCounter(currentCmd);
                    log.debug("Executing SQL: " + currentCmd);
                    statement.executeUpdate(currentCmd);
                }
            } finally {
                // make sure to close the connection and statement before
                // committing the transaction or XA will break
                JDBCUtil.safeClose(statement);
                JDBCUtil.safeClose(con);
            }
        } catch (Exception e) {
            throw MESSAGES.errorInPostTableCreate(e);
        } finally {
            try {
                // resume the old transaction
                if (oldTransaction != null) {
                    tm.resume(oldTransaction);
                }
            } catch (Exception e) {
                throw MESSAGES.couldNotReattachAfterPostTableCreate(e);
            }
        }

        // success
        log.debug("Issued SQL  " + sql + " successfully.");
    }

    private String getEntityCreateTableSQL(DataSource dataSource) {
        StringBuffer sql = new StringBuffer();
        sql.append(SQLUtil.CREATE_TABLE).append(entity.getQualifiedTableName()).append(" (");

        // add fields
        boolean comma = false;
        JDBCFieldBridge[] fields = entity.getTableFields();
        for (int i = 0; i < fields.length; ++i) {
            JDBCFieldBridge field = fields[i];
            JDBCType type = field.getJDBCType();
            if (comma) {
                sql.append(SQLUtil.COMMA);
            } else {
                comma = true;
            }
            addField(type, sql);
        }

        // add a pk constraint
        if (entityMetaData.hasPrimaryKeyConstraint()) {
            JDBCFunctionMappingMetaData pkConstraint = manager.getMetaData().getTypeMapping().getPkConstraintTemplate();
            if (pkConstraint == null) {
                throw CmpMessages.MESSAGES.pkNotAllowedForDatasource();
            }

            String defTableName = entity.getManager().getMetaData().getDefaultTableName();
            String name = "pk_" + SQLUtil.unquote(defTableName, dataSource);
            name = SQLUtil.fixConstraintName(name, dataSource);
            String[] args = new String[]{
                    name,
                    SQLUtil.getColumnNamesClause(entity.getPrimaryKeyFields(), new StringBuffer(100)).toString()
            };
            sql.append(SQLUtil.COMMA);
            pkConstraint.getFunctionSql(args, sql);
        }

        return sql.append(')').toString();
    }

    /**
     * Create indices for the fields in the table that have a
     * &lt;dbindex&gt; tag in jbosscmp-jdbc.xml
     *
     * @param dataSource
     * @
     */
    private void createCMPIndices(DataSource dataSource, ArrayList indexNames) {
        // Only create indices on CMP fields
        JDBCFieldBridge[] cmpFields = entity.getTableFields();
        for (int i = 0; i < cmpFields.length; ++i) {
            JDBCFieldBridge field = cmpFields[i];
            JDBCCMPFieldMetaData fieldMD = entity.getMetaData().getCMPFieldByName(field.getFieldName());

            if (fieldMD != null && fieldMD.isIndexed()) {
                createCMPIndex(dataSource, field, indexNames);
            }
        }

        final JDBCAbstractCMRFieldBridge[] cmrFields = entity.getCMRFields();
        if (cmrFields != null) {
            for (int i = 0; i < cmrFields.length; ++i) {
                JDBCAbstractCMRFieldBridge cmrField = cmrFields[i];
                if (cmrField.getRelatedCMRField().getMetaData().isIndexed()) {
                    final JDBCFieldBridge[] fkFields = cmrField.getForeignKeyFields();
                    if (fkFields != null) {
                        for (int fkInd = 0; fkInd < fkFields.length; ++fkInd) {
                            createCMPIndex(dataSource, fkFields[fkInd], indexNames);
                        }
                    }
                }
            }
        }
    }

    /**
     * Create index for one specific field
     *
     * @param dataSource
     * @param field      to create index for
     * @
     */
    private void createCMPIndex(DataSource dataSource, JDBCFieldBridge field, ArrayList indexNames) {
        StringBuffer sql;
        log.debug("Creating index for field " + field.getFieldName());
        sql = new StringBuffer();
        sql.append(SQLUtil.CREATE_INDEX);
        String indexName;
        boolean indexExists;
        do {
            indexName = entity.getQualifiedTableName() + IDX_POSTFIX + idxCount;
            idxCount++;
            indexExists = false;
            if (indexNames != null) {
                for (int i = 0; i < indexNames.size() && !indexExists; i++) {
                    indexExists = indexName.equalsIgnoreCase(((String) indexNames.get(i)));
                }
            }
        }
        while (indexExists);

        sql.append(indexName);
        sql.append(SQLUtil.ON);
        sql.append(entity.getQualifiedTableName() + " (");
        SQLUtil.getColumnNamesClause(field, sql);
        sql.append(")");

        createIndex(dataSource, entity.getQualifiedTableName(), indexName, sql.toString());
    }

    private void createCMRIndex(DataSource dataSource, JDBCAbstractCMRFieldBridge field) {
        JDBCRelationMetaData rmd;
        String tableName;

        rmd = field.getMetaData().getRelationMetaData();

        if (rmd.isTableMappingStyle()) {
            tableName = rmd.getDefaultTableName();
            createFKIndex(rmd.getLeftRelationshipRole(), dataSource, tableName);
            createFKIndex(rmd.getRightRelationshipRole(), dataSource, tableName);
        } else if (field.hasForeignKey()) {
            tableName = field.getEntity().getQualifiedTableName();
            createFKIndex(field.getRelatedCMRField().getMetaData(), dataSource, tableName);
        }
    }

    private void createFKIndex(JDBCRelationshipRoleMetaData metadata, DataSource dataSource, String tableName) {
        Collection kfl = metadata.getKeyFields();
        Iterator it = kfl.iterator();
        while (it.hasNext()) {
            JDBCCMPFieldMetaData fi = (JDBCCMPFieldMetaData) it.next();
            if (metadata.isIndexed()) {
                createIndex(dataSource, tableName, fi.getFieldName(), createIndexSQL(fi, tableName));
                idxCount++;
            }
        }
    }

    private String createIndexSQL(JDBCCMPFieldMetaData fi, String tableName) {
        StringBuffer sql = new StringBuffer();
        sql.append(SQLUtil.CREATE_INDEX);
        sql.append(tableName + IDX_POSTFIX + idxCount);
        sql.append(SQLUtil.ON);
        sql.append(tableName + " (");
        sql.append(fi.getColumnName());
        sql.append(')');
        return sql.toString();
    }

    private void addField(JDBCType type, StringBuffer sqlBuffer) {
        // apply auto-increment template
        if (type.getAutoIncrement()[0]) {
            String columnClause = SQLUtil.getCreateTableColumnsClause(type);
            JDBCFunctionMappingMetaData autoIncrement =
                    manager.getMetaData().getTypeMapping().getAutoIncrementTemplate();
            if (autoIncrement == null) {
                throw MESSAGES.autoIncTemplateNotFound();
            }
            String[] args = new String[]{columnClause};
            autoIncrement.getFunctionSql(args, sqlBuffer);
        } else {
            sqlBuffer.append(SQLUtil.getCreateTableColumnsClause(type));
        }
    }

    private String getRelationCreateTableSQL(JDBCAbstractCMRFieldBridge cmrField,
                                             DataSource dataSource) {
        JDBCFieldBridge[] leftKeys = cmrField.getTableKeyFields();
        JDBCFieldBridge[] rightKeys = cmrField.getRelatedCMRField().getTableKeyFields();
        JDBCFieldBridge[] fieldsArr = new JDBCFieldBridge[leftKeys.length + rightKeys.length];
        System.arraycopy(leftKeys, 0, fieldsArr, 0, leftKeys.length);
        System.arraycopy(rightKeys, 0, fieldsArr, leftKeys.length, rightKeys.length);

        StringBuffer sql = new StringBuffer();
        sql.append(SQLUtil.CREATE_TABLE).append(cmrField.getQualifiedTableName())
                .append(" (")
                        // add field declaration
                .append(SQLUtil.getCreateTableColumnsClause(fieldsArr));

        // add a pk constraint
        final JDBCRelationMetaData relationMetaData = cmrField.getMetaData().getRelationMetaData();
        if (relationMetaData.hasPrimaryKeyConstraint()) {
            JDBCFunctionMappingMetaData pkConstraint =
                    manager.getMetaData().getTypeMapping().getPkConstraintTemplate();
            if (pkConstraint == null) {
                throw MESSAGES.pkConstraintNotAllowed();
            }

            String name = "pk_" + relationMetaData.getDefaultTableName();
            name = SQLUtil.fixConstraintName(name, dataSource);
            String[] args = new String[]{
                    name,
                    SQLUtil.getColumnNamesClause(fieldsArr, new StringBuffer(100).toString(), new StringBuffer()).toString()
            };
            sql.append(SQLUtil.COMMA);
            pkConstraint.getFunctionSql(args, sql);
        }
        sql.append(')');
        return sql.toString();
    }

    private void addForeignKeyConstraint(JDBCAbstractCMRFieldBridge cmrField) {
        JDBCRelationshipRoleMetaData metaData = cmrField.getMetaData();
        if (metaData.hasForeignKeyConstraint()) {
            if (metaData.getRelationMetaData().isTableMappingStyle()) {
                addForeignKeyConstraint(manager.getDataSource(metaData.getRelationMetaData().getDataSourceName()), // TODO: jeb - get datasource
                        cmrField.getQualifiedTableName(),
                        cmrField.getFieldName(),
                        cmrField.getTableKeyFields(),
                        cmrField.getEntity().getQualifiedTableName(),
                        cmrField.getEntity().getPrimaryKeyFields());

            } else if (cmrField.hasForeignKey()) {
                JDBCAbstractEntityBridge relatedEntity = (JDBCAbstractEntityBridge) cmrField.getRelatedEntity();
                addForeignKeyConstraint(cmrField.getEntity().getDataSource(),
                        cmrField.getEntity().getQualifiedTableName(),
                        cmrField.getFieldName(),
                        cmrField.getForeignKeyFields(),
                        relatedEntity.getQualifiedTableName(),
                        relatedEntity.getPrimaryKeyFields());
            }
        } else {
            log.debug("Foreign key constraint not added as requested: relationshipRolename=" + metaData.getRelationshipRoleName());
        }
    }

    private void addForeignKeyConstraint(DataSource dataSource,
                                         String tableName,
                                         String cmrFieldName,
                                         JDBCFieldBridge[] fields,
                                         String referencesTableName,
                                         JDBCFieldBridge[] referencesFields) {
        // can only alter tables we created
        if (!manager.hasCreateTable(tableName)) {
            return;
        }

        JDBCFunctionMappingMetaData fkConstraint = manager.getMetaData().getTypeMapping().getFkConstraintTemplate();
        if (fkConstraint == null) {
            throw MESSAGES.fkConstraintNotAllowed();
        }
        String a = SQLUtil.getColumnNamesClause(fields, new StringBuffer(50)).toString();
        String b = SQLUtil.getColumnNamesClause(referencesFields, new StringBuffer(50)).toString();

        String[] args = new String[]{
                tableName,
                SQLUtil.fixConstraintName("fk_" + tableName + "_" + cmrFieldName, dataSource),
                a,
                referencesTableName,
                b};

        String sql = fkConstraint.getFunctionSql(args, new StringBuffer(100)).toString();

        // since we use the pools, we have to do this within a transaction
        // suspend the current transaction
        TransactionManager tm = manager.getComponent().getTransactionManager();
        Transaction oldTransaction;
        try {
            oldTransaction = tm.suspend();
        } catch (Exception e) {
            throw MESSAGES.couldNotSuspendBeforeFk(e);
        }

        try {
            Connection con = null;
            Statement statement = null;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Executing SQL: " + sql);
                }
                con = dataSource.getConnection();
                statement = con.createStatement();
                statement.executeUpdate(sql);
            } finally {
                // make sure to close the connection and statement before
                // committing the transaction or XA will break
                JDBCUtil.safeClose(statement);
                JDBCUtil.safeClose(con);
            }
        } catch (Exception e) {
            throw MESSAGES.errorAddingFk(tableName, e);
        } finally {
            try {
                // resume the old transaction
                if (oldTransaction != null) {
                    tm.resume(oldTransaction);
                }
            } catch (Exception e) {
                throw MESSAGES.couldNotReattachAfterCreateIndex(e);
            }
        }
    }


    /**
     * Replace %%t in the sql command with the current table name
     *
     * @param in    sql statement with possible %%t to substitute with table name
     * @param table the table name
     * @return String with sql statement
     */
    private static String replaceTable(String in, String table) {
        int pos;

        pos = in.indexOf("%%t");
        // No %%t -> return input
        if (pos == -1) {
            return in;
        }

        String first = in.substring(0, pos);
        String last = in.substring(pos + 3);

        return first + table + last;
    }

    /**
     * Replace %%n in the sql command with a running (index) number
     *
     * @param in
     * @return
     */
    private String replaceIndexCounter(String in) {
        int pos;

        pos = in.indexOf("%%n");
        // No %%n -> return input
        if (pos == -1) {
            return in;
        }

        String first = in.substring(0, pos);
        String last = in.substring(pos + 3);
        idxCount++;
        return first + idxCount + last;
    }
}
