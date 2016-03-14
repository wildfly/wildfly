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
package org.jboss.as.test.integration.ejb.transaction.bmt.lazyenlist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ATMBean implements ATM {
    private static final Logger log = Logger.getLogger(ATMBean.class);

    @PersistenceContext
    private EntityManager em;

    @Resource
    private UserTransaction ut;

    @Resource(mappedName = "java:jboss/datasources/ExampleDS")
    private DataSource ds;

    private void beginTx() {
        try {
            ut.begin();
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private void commitTx() {
        try {
            ut.commit();
        } catch (RuntimeException e) {
            throw e;
        }
        // RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long createAccount(double balance) {
        beginTx();
        try {
            Account a = new Account();
            a.setBalance(balance);
            em.persist(a);
            commitTx();
            return a.getId();
        } finally {
            rollbackTxIfNeeded();
        }
    }

    public double getBalance(long id) {
        Account a = em.find(Account.class, id);
        return a.getBalance();
    }

    private void rollbackTx() {
        try {
            ut.rollback();
        } catch (RuntimeException e) {
            throw e;
        }
        // SecurityException, SystemException
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void rollbackTxIfNeeded() {
        try {
            switch (ut.getStatus()) {
                case Status.STATUS_COMMITTED:
                case Status.STATUS_NO_TRANSACTION:
                    log.infov("Transaction {} is not active thus won't be rollbacked", ut);
                    break;
                default:
                    log.infov("Transaction {} is active and going to be rollbacked", ut);
                    ut.rollback();
            }
        } catch (RuntimeException e) {
            throw e;
        }
        // SecurityException, SystemException
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public double depositTwice(long id, double a1, double a2) {
        Account a;
        beginTx();
        try {
            a = em.find(Account.class, id);
            double balanceBefore = a.getBalance();
            log.infov("Depositing '{}' to account id '{}' with start balance '{}'",
                    a1, a.getId(), balanceBefore);
            // unsafe (nolock)
            a.setBalance(balanceBefore + a1);
            em.flush();
            commitTx();
        } finally {
            rollbackTxIfNeeded();
        }

        beginTx();
        try {
            a = em.find(Account.class, id);
            double balanceBefore = a.getBalance();
            log.infov("Depositing '{}' to account id '{}' with start balance '{}'",
                    a2, a.getId(), balanceBefore);
            // unsafe (nolock)
            a.setBalance(balanceBefore + a2);
            em.flush();
            commitTx();
        } finally {
            rollbackTxIfNeeded();
        }
        return a.getBalance();
    }

    public double depositTwiceRawSQL(long id, double a1, double a2) {
        Connection conn = null;
        ResultSet rs = null;
        double balance;

        try {
            try {
                conn = ds.getConnection();
                PreparedStatement psSelect = conn.prepareStatement("SELECT balance FROM account WHERE id = ?");

                psSelect.setLong(1, id);
                rs = psSelect.executeQuery();
                if (!rs.next())
                    throw new IllegalArgumentException("can't find account " + id);
                balance = rs.getDouble(1);
            } finally {
                if(rs != null) {
                    rs.close();
                }
            }

            PreparedStatement ps = conn.prepareStatement("UPDATE account SET balance = ? WHERE id = ?");
            beginTx();
            try {
                balance += a1;
                ps.setDouble(1, balance);
                ps.setLong(2, id);
                int rows = ps.executeUpdate();
                if (rows != 1)
                    throw new IllegalStateException("first update failed");

                commitTx();
            } finally {
                ps.close();
                rollbackTxIfNeeded();
            }

            ps = conn.prepareStatement("UPDATE account SET balance = ? WHERE id = ?");
            beginTx();
            try {
                balance += a2;
                ps.setDouble(1, balance);
                ps.setLong(2, id);
                int rows = ps.executeUpdate();
                if (rows != 1)
                    throw new IllegalStateException("second update failed");

                commitTx();
            } finally {
                ps.close();
                rollbackTxIfNeeded();
            }

            return balance;
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        } finally {
            try {
                if(conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                log.errorv("Final closing of connection {} was unsuccesful", conn, e);
            }
        }
    }

    public double depositTwiceWithRollback(long id, double a1, double a2) {
        Account a;
        beginTx();
        try {
            a = em.find(Account.class, id);
            // unsafe (nolock)
            a.setBalance(a.getBalance() + a1);
            em.flush();
            commitTx();
        } finally {
            rollbackTxIfNeeded();
        }

        beginTx();
        try {
            // unsafe
            a.setBalance(a.getBalance() + a2);
            em.flush();
        } finally {
            rollbackTx();
        }
        return a.getBalance();
    }

    /**
     * Do the same, but then raw sql.
     *
     * @param id
     * @param a1
     * @param a2
     */
    public double withdrawTwiceWithRollback(long id, double a1, double a2) {
        try {
            Connection conn = ds.getConnection();
            PreparedStatement psSelect = conn.prepareStatement("SELECT balance FROM account WHERE id = ?");
            double balance;
            try {
                psSelect.setLong(1, id);
                ResultSet rs = psSelect.executeQuery();
                if (!rs.next())
                    throw new IllegalArgumentException("can't find account " + id);
                balance = rs.getDouble(1);

                rs.close();
                psSelect.close();

                PreparedStatement ps = conn.prepareStatement("UPDATE account SET balance = ? WHERE id = ?");
                try {
                    beginTx();
                    try {
                        balance -= a1;
                        ps.setDouble(1, balance);
                        ps.setLong(2, id);
                        int rows = ps.executeUpdate();
                        if (rows != 1)
                            throw new IllegalStateException("first update failed");

                        ps.close();
                        conn.close();

                        commitTx();
                    } finally {
                        rollbackTxIfNeeded();
                    }

                    conn = ds.getConnection();
                    ps = conn.prepareStatement("UPDATE account SET balance = ? WHERE id = ?");

                    beginTx();
                    try {
                        balance -= a2;
                        ps.setDouble(1, balance);
                        ps.setLong(2, id);
                        int rows = ps.executeUpdate();
                        if (rows != 1)
                            throw new IllegalStateException("second update failed");
                    } finally {
                        rollbackTx();
                    }
                } finally {
                    ps.close();
                }

                return balance;
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
