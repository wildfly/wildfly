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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.keygenerator.KeyGenerator;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class HiLoKeyGenerator implements KeyGenerator {
    private static long highestHi = 0;

    public static synchronized long getHighestHi() {
        return highestHi;
    }

    public static synchronized void setHighestHi(long highestHi) {
        HiLoKeyGenerator.highestHi = highestHi;
    }

    private final Logger log;
    private final DataSource ds;
    private final long blockSize;

    private long hi;
    private long lo;

    private TransactionManager tm;
    private String updateHiSql;
    private String selectHiSql;

    public HiLoKeyGenerator(
            DataSource ds,
            String tableName,
            String sequenceColumn,
            String sequenceName,
            String idColumnName,
            String selectHiSql,
            long blockSize,
            TransactionManager tm
    ) {
        this.ds = ds;
        this.blockSize = blockSize;
        this.tm = tm;
        this.log = Logger.getLogger(getClass().getName() + "#" + tableName + "_" + sequenceName);

        updateHiSql = "update " +
                tableName +
                " set " +
                idColumnName +
                "=?" +
                " where " + sequenceColumn + "='" + sequenceName + "' and " +
                idColumnName + "=?";

        this.selectHiSql = selectHiSql;
    }

    public synchronized Object generateKey() {
        if (lo < hi) {
            ++lo;
        } else {
            Transaction curTx = null;
            try {
                curTx = tm.suspend();
            } catch (SystemException e) {
                throw new IllegalStateException("Failed to suspend current transaction.");
            }

            try {
                tm.begin();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to begin a new transaction.");
            }

            try {
                doGenerate();
                tm.commit();
            } catch (SQLException e) {
                log.error("Failed to update table: " + e.getMessage(), e);

                try {
                    tm.rollback();
                } catch (SystemException e1) {
                    log.error("Failed to rollback.", e1);
                }

                throw new IllegalStateException(e.getMessage());
            } catch (Exception e) {
                log.error("Failed to commit.", e);
            } finally {
                if (curTx != null) {
                    try {
                        tm.resume(curTx);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to resume transaction: " + e.getMessage());
                    }
                }
            }
        }

        return new Long(lo);
    }

    private void doGenerate() throws SQLException {
        long curHi;
        do {
            curHi = getCurrentHi();
            lo = curHi + 1;
            hi = curHi + blockSize;
        }
        while (!updateHi(curHi, hi));
    }

    private long getCurrentHi() throws SQLException {
        return selectHiSql != null ? selectHi() : getHighestHi();
    }

    private boolean updateHi(long curHi, long newHi) throws SQLException {
        if (selectHiSql == null) {
            setHighestHi(newHi);
        }
        return updateTable(curHi, newHi);
    }

    private long selectHi() throws SQLException {
        Connection con = null;
        PreparedStatement selectHiSt = null;
        ResultSet rs = null;

        if (log.isTraceEnabled()) {
            log.trace("Executing SQL: " + selectHiSql);
        }

        try {
            con = ds.getConnection();
            selectHiSt = con.prepareStatement(selectHiSql);
            rs = selectHiSt.executeQuery();
            if (!rs.next()) {
                throw new IllegalStateException("The sequence has not been initialized in the service start phase!");
            }
            return rs.getLong(1);
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(selectHiSt);
            JDBCUtil.safeClose(con);
        }
    }

    private boolean updateTable(long curHi, long newHi) throws SQLException {
        Connection con = null;
        PreparedStatement updateHi = null;

        if (log.isTraceEnabled()) {
            log.trace("Executing SQL: " + updateHiSql + ", [" + newHi + "," + curHi + "]");
        }

        try {
            con = ds.getConnection();
            updateHi = con.prepareStatement(updateHiSql);
            updateHi.setLong(1, newHi);
            updateHi.setLong(2, curHi);
            return updateHi.executeUpdate() == 1;
        } finally {
            JDBCUtil.safeClose(updateHi);
            JDBCUtil.safeClose(con);
        }
    }
}
