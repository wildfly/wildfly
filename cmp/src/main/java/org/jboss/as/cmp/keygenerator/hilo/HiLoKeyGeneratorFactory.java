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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.keygenerator.KeyGenerator;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactory;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 * @jmx.mbean name="jboss.system:service=KeyGeneratorFactory,type=HiLo"
 * extends="org.jboss.system.ServiceMBean"
 */
public class HiLoKeyGeneratorFactory implements KeyGeneratorFactory, Serializable {
    private static final Logger log = Logger.getLogger(HiLoKeyGeneratorFactory.class);
    private String dataSourceJndi;
    private transient DataSource ds;
    private transient TransactionManager tm;

    private String jndiName;
    private String tableName;
    private String sequenceColumn;
    private String sequenceName;
    private String idColumnName;
    private String createTableDdl;
    private String selectHiSql;
    private long blockSize;

    private boolean createTable = true;
    private boolean dropTable;

    public String getFactoryName() {
        return jndiName;
    }


    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) throws Exception {
//        if (getState() == STARTED && !tableName.equals(this.tableName)) {
//            initSequence(tableName, sequenceColumn, sequenceName, idColumnName);
//        }
        this.tableName = tableName;
    }

    public String getSequenceColumn() {
        return sequenceColumn;
    }

    public void setSequenceColumn(String sequenceColumn) {
        this.sequenceColumn = sequenceColumn;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public String getIdColumnName() {
        return idColumnName;
    }

    public void setIdColumnName(String idColumnName) {
        this.idColumnName = idColumnName;
    }

    public String getCreateTableDdl() {
        return createTableDdl;
    }

    public void setCreateTableDdl(String createTableDdl) {
        this.createTableDdl = createTableDdl;
    }

    public String getSelectHiSql() {
        return selectHiSql;
    }

    public void setSelectHiSql(String selectHiSql) {
        this.selectHiSql = selectHiSql;
    }

    public long getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(long blockSize) {
        this.blockSize = blockSize;
    }

    public boolean isCreateTable() {
        return createTable;
    }

    public void setCreateTable(boolean createTable) {
        this.createTable = createTable;
    }

    public boolean isDropTable() {
        return dropTable;
    }

    public void setDropTable(boolean dropTable) {
        this.dropTable = dropTable;
    }

    // KeyGeneratorFactory implementation

    public KeyGenerator getKeyGenerator() throws Exception {
        return new HiLoKeyGenerator(ds, tableName, sequenceColumn, sequenceName, idColumnName, selectHiSql, blockSize, tm);
    }

    // Private

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

            con = ds.getConnection();
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
            if (!SQLUtil.tableExists(tableName, ds)) {
                log.debug("Executing DDL: " + createTableDdl);

                con = ds.getConnection();
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
            if (SQLUtil.tableExists(tableName, ds)) {
                final String ddl = "drop table " + tableName;
                log.debug("Executing DDL: " + ddl);

                con = ds.getConnection();
                st = con.createStatement();
                st.executeUpdate(ddl);
            }
        } finally {
            JDBCUtil.safeClose(st);
            JDBCUtil.safeClose(con);
        }
    }

    private DataSource lookupDataSource(String dataSource) throws Exception {
        try {
            return (DataSource) new InitialContext().lookup(dataSource);
        } catch (NamingException e) {
            throw new Exception("Failed to lookup data source: " + dataSource);
        }
    }
}
