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
package org.jboss.as.cmp.keygenerator.hilo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.keygenerator.KeyGenerator;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactory;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 * extends="org.jboss.system.ServiceMBean"
 */
public class HiLoKeyGeneratorFactory implements KeyGeneratorFactory, Service<KeyGeneratorFactory> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("cmp", "keygen", HiLoKeyGeneratorFactory.class.getSimpleName());

    private static final Logger log = Logger.getLogger(HiLoKeyGeneratorFactory.class);

    private final InjectedValue<DataSource> ds = new InjectedValue<DataSource>();
    private final InjectedValue<TransactionManager> tm = new InjectedValue<TransactionManager>();

    private String tableName;
    private String sequenceColumn;
    private String sequenceName;
    private String idColumnName;
    private String createTableDdl;
    private String selectHiSql;
    private long blockSize;

    private boolean createTable = true;
    private boolean dropTable;

    public synchronized void start(StartContext context) throws StartException {
        try {
            initSequence(tableName, sequenceColumn, sequenceName, idColumnName);
        } catch (SQLException e) {
            throw new StartException("Failed to start HiLoKeyGeneratorFactory", e);
        }
    }

    public synchronized void stop(StopContext context) {
        if (dropTable) {
            try {
                dropTableIfExists(tableName);
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to stop HiLoKeyGeneratorFactory", e);
            }
        }
    }

    public KeyGeneratorFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public KeyGenerator getKeyGenerator() throws Exception {
        return new HiLoKeyGenerator(ds.getValue(), tableName, sequenceColumn, sequenceName, idColumnName, selectHiSql, blockSize, tm.getValue());
    }

    private void initSequence(String tableName, String sequenceColumn, String sequenceName, String idColumnName) throws SQLException {
        if (createTable) {
            createTableIfNotExists(tableName);
        }

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            String sql = "select " + idColumnName + " from " + tableName + " where " + sequenceColumn + "='" + sequenceName + "'";
            log.debug("Executing SQL: " + sql);

            con = ds.getValue().getConnection();
            st = con.createStatement();
            rs = st.executeQuery(sql);
            if (!rs.next()) {
                sql = "insert into " +
                        tableName +
                        "(" +
                        sequenceColumn +
                        ", " +
                        idColumnName +
                        ") values ('" + sequenceName + "', 0)";
                log.debug("Executing SQL: " + sql);

                final Statement insertSt = con.createStatement();
                try {
                    final int i = insertSt.executeUpdate(sql);
                    if (i != 1) {
                        throw new SQLException("Expected one updated row but got: " + i);
                    }
                } finally {
                    JDBCUtil.safeClose(insertSt);
                }
            } else {
                HiLoKeyGenerator.setHighestHi(rs.getLong(1));
            }
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(st);
            JDBCUtil.safeClose(con);
        }
    }

    private void createTableIfNotExists(String tableName) throws SQLException {
        Connection con = null;
        Statement st = null;
        try {
            if (!SQLUtil.tableExists(tableName, ds.getValue())) {
                log.debug("Executing DDL: " + createTableDdl);

                con = ds.getValue().getConnection();
                st = con.createStatement();
                st.executeUpdate(createTableDdl);
            }
        } finally {
            JDBCUtil.safeClose(st);
            JDBCUtil.safeClose(con);
        }
    }

    private void dropTableIfExists(String tableName) throws SQLException {
        Connection con = null;
        Statement st = null;
        try {
            if (SQLUtil.tableExists(tableName, ds.getValue())) {
                final String ddl = "drop table " + tableName;
                log.debug("Executing DDL: " + ddl);

                con = ds.getValue().getConnection();
                st = con.createStatement();
                st.executeUpdate(ddl);
            }
        } finally {
            JDBCUtil.safeClose(st);
            JDBCUtil.safeClose(con);
        }
    }

    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    public void setSequenceColumn(final String sequenceColumn) {
        this.sequenceColumn = sequenceColumn;
    }

    public void setSequenceName(final String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public void setIdColumnName(final String idColumnName) {
        this.idColumnName = idColumnName;
    }

    public void setCreateTableDdl(final String createTableDdl) {
        this.createTableDdl = createTableDdl;
    }

    public void setSelectHiSql(final String selectHiSql) {
        this.selectHiSql = selectHiSql;
    }

    public void setBlockSize(final long blockSize) {
        this.blockSize = blockSize;
    }

    public void setCreateTable(final boolean createTable) {
        this.createTable = createTable;
    }

    public void setDropTable(final boolean dropTable) {
        this.dropTable = dropTable;
    }

    public Injector<TransactionManager> getTransactionManagerInjector() {
        return tm;
    }

    public Injector<DataSource> getDataSourceInjector() {
        return ds;
    }
}
