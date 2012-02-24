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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.CmpLogger;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractEntityBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationMetaData;
import org.jboss.logging.Logger;


/**
 * JDBCStopCommand drops the table for this entity if specified in the xml.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Oberg</a>
 * @author <a href="mailto:justin@j-m-f.demon.co.uk">Justin Forder</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCStopCommand {
    private final JDBCEntityPersistenceStore manager;
    private final JDBCAbstractEntityBridge entity;
    private final JDBCEntityMetaData entityMetaData;
    private final Logger log;

    public JDBCStopCommand(JDBCEntityPersistenceStore manager) {
        this.manager = manager;
        entity = manager.getEntityBridge();
        entityMetaData = entity.getMetaData();

        // Create the Log
        log = Logger.getLogger(
                this.getClass().getName() +
                        "." +
                        manager.getMetaData().getName());
    }

    public boolean execute() {
        boolean success = true;

        // drop relation tables
        JDBCAbstractCMRFieldBridge[] cmrFields = entity.getCMRFields();
        for (int i = 0; i < cmrFields.length; ++i) {
            JDBCAbstractCMRFieldBridge cmrField = cmrFields[i];
            JDBCRelationMetaData relationMetaData = cmrField.getMetaData().getRelationMetaData();
            if (relationMetaData.isTableMappingStyle() && !relationMetaData.isTableDropped()) {
                if (relationMetaData.getRemoveTable()) {
                    final boolean dropped = dropTable(manager.getDataSource(relationMetaData.getDataSourceName()), cmrField.getQualifiedTableName());
                    if (!dropped) {
                        success = false;
                    } else {
                        relationMetaData.setTableDropped();
                    }
                }
            }
        }

        if (entityMetaData.getRemoveTable()) {
            boolean dropped = dropTable(entity.getDataSource(), entity.getQualifiedTableName());
            if (!dropped) {
                success = false;
            }
        }

        return success;
    }

    private boolean dropTable(DataSource dataSource, String qualifiedTableName) {
        boolean success;
        Connection con = null;
        ResultSet rs = null;

        String schema = SQLUtil.getSchema(qualifiedTableName);
        String tableName = schema != null ? SQLUtil.getTableNameWithoutSchema(qualifiedTableName) : qualifiedTableName;

        // was the table already delete?
        try {
            con = dataSource.getConnection();
            DatabaseMetaData dmd = con.getMetaData();
            rs = dmd.getTables(con.getCatalog(), schema, tableName, null);
            if (!rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            log.debug("Error getting database metadata for DROP TABLE command. " +
                    " DROP TABLE will not be executed. ", e);
            return true;
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(con);
        }

        // since we use the pools, we have to do this within a transaction

        // suspend the current transaction
        TransactionManager tm = manager.getComponent().getTransactionManager();
        Transaction oldTransaction = null;
        try {
            oldTransaction = tm.suspend();
        } catch (Exception e) {
            CmpLogger.ROOT_LOGGER.couldNotSuspendTxBeforeDrop(qualifiedTableName, e);
        }

        try {
            Statement statement = null;
            try {
                con = dataSource.getConnection();
                statement = con.createStatement();

                // execute sql
                String sql = SQLUtil.DROP_TABLE + qualifiedTableName;
                log.debug("Executing SQL: " + sql);
                statement.executeUpdate(sql);
                success = true;
            } finally {
                JDBCUtil.safeClose(statement);
                JDBCUtil.safeClose(con);
            }
        } catch (Exception e) {
            log.debug("Could not drop table " + qualifiedTableName + ": " + e.getMessage());
            success = false;
        } finally {
            try {
                // resume the old transaction
                if (oldTransaction != null) {
                    tm.resume(oldTransaction);
                }
            } catch (Exception e) {
                CmpLogger.ROOT_LOGGER.couldNotReattachAfterDrop();
            }
        }

        return success;
    }
}
