/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.tx;

import static org.jboss.as.ejb3.tx.util.StatusHelper.statusAsString;

import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.NoSuchEntityException;
import javax.ejb.TransactionAttributeType;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.MethodIntfHelper;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * Ensure the correct exceptions are thrown based on both caller
 * transactional context and supported Transaction Attribute Type
 * <p/>
 * EJB3 13.6.2.6
 * EJB3 Core Specification 14.3.1 Table 14
 *
 * @author <a href="mailto:andrew.rubinger@redhat.com">ALR</a>
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Scott Marlow
 */
public class CMTTxInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new CMTTxInterceptor());


    /**
     * The <code>endTransaction</code> method ends a transaction and
     * translates any exceptions into
     * TransactionRolledBack[Local]Exception or SystemException.
     *
     * @param tx a <code>Transaction</code> value
     */
    protected void endTransaction(final Transaction tx) {
        ContextTransactionManager tm = ContextTransactionManager.getInstance();
        try {
            if (! tx.equals(tm.getTransaction())) {
                throw EjbLogger.ROOT_LOGGER.wrongTxOnThread(tx, tm.getTransaction());
            }
            final int txStatus = tx.getStatus();
            if (txStatus == Status.STATUS_ACTIVE) {
                // Commit tx
                // This will happen if
                // a) everything goes well
                // b) app. exception was thrown
                tm.commit();
            } else if (txStatus == Status.STATUS_MARKED_ROLLBACK) {
                tm.rollback();
            } else if (txStatus == Status.STATUS_ROLLEDBACK || txStatus == Status.STATUS_ROLLING_BACK) {
                // handle reaper canceled (rolled back) tx case (see WFLY-1346)
                // clear current tx state and throw RollbackException (EJBTransactionRolledbackException)
                tm.rollback();
                throw EjbLogger.ROOT_LOGGER.transactionAlreadyRolledBack(tx);
            } else if (txStatus == Status.STATUS_UNKNOWN) {
                // STATUS_UNKNOWN isn't expected to be reached here but if it does, we need to clear current thread tx.
                // It is possible that calling tm.commit() could succeed but we call tm.rollback, since this is an unexpected
                // tx state that are are handling.
                tm.rollback();
                // if the tm.rollback doesn't fail, we throw an EJBException to reflect the unexpected tx state.
                throw EjbLogger.ROOT_LOGGER.transactionInUnexpectedState(tx, statusAsString(txStatus));
            } else {
                // logically, all of the following (unexpected) tx states are handled here:
                //  Status.STATUS_PREPARED
                //  Status.STATUS_PREPARING
                //  Status.STATUS_COMMITTING
                //  Status.STATUS_NO_TRANSACTION
                //  Status.STATUS_COMMITTED
                tm.suspend();                       // clear current tx state and throw EJBException
                throw EjbLogger.ROOT_LOGGER.transactionInUnexpectedState(tx, statusAsString(txStatus));
            }
        } catch (RollbackException e) {
            throw new EJBTransactionRolledbackException("Transaction rolled back", e);
        } catch (HeuristicMixedException | SystemException | HeuristicRollbackException e) {
            throw new EJBException(e);
        }
    }

    public Exception handleExceptionInOurTx(InterceptorContext invocation, Throwable t, Transaction tx, final EJBComponent component) {
        ApplicationExceptionDetails ae = component.getApplicationException(t.getClass(), invocation.getMethod());
        if (ae != null) {
            if (ae.isRollback()) setRollbackOnly(tx);
            return (Exception) t;
        }
        try {
            throw t;
        } catch (EJBException | RemoteException e) {
            setRollbackOnly(tx);
            return e;
        } catch (RuntimeException e) {
            setRollbackOnly(tx);
            return new EJBException(e);
        } catch (Error e) {
            setRollbackOnly(tx);
            return EjbLogger.ROOT_LOGGER.unexpectedError(e);
        } catch (Exception e) {
            // an application exception
            return e;
        } catch (Throwable e) {
            throw new EJBException(new UndeclaredThrowableException(e));
        }
    }

    public Object processInvocation(InterceptorContext invocation) throws Exception {
        final EJBComponent component = (EJBComponent) invocation.getPrivateData(Component.class);
        final ContextTransactionManager tm = ContextTransactionManager.getInstance();
        final int oldTimeout = tm.getTransactionTimeout();
        try {
            final MethodIntf methodIntf = MethodIntfHelper.of(invocation);
            final TransactionAttributeType attr = component.getTransactionAttributeType(methodIntf, invocation.getMethod());
            final int timeoutInSeconds = component.getTransactionTimeout(methodIntf, invocation.getMethod());
            switch (attr) {
                case MANDATORY:
                    return mandatory(invocation, component);
                case NEVER:
                    return never(invocation, component);
                case NOT_SUPPORTED:
                    return notSupported(invocation, component);
                case REQUIRED:
                    return required(invocation, component, timeoutInSeconds);
                case REQUIRES_NEW:
                    return requiresNew(invocation, component, timeoutInSeconds);
                case SUPPORTS:
                    return supports(invocation, component);
                default:
                    throw EjbLogger.ROOT_LOGGER.unknownTxAttributeOnInvocation(attr, invocation);
            }
        } finally {
            // See also https://issues.jboss.org/browse/WFTC-44
            tm.setTransactionTimeout(oldTimeout == ContextTransactionManager.getGlobalDefaultTransactionTimeout() ? 0 : oldTimeout);
        }
    }

    protected Object invokeInCallerTx(InterceptorContext invocation, Transaction tx, final EJBComponent component) throws Exception {
        try {
            return invocation.proceed();
        } catch (Error e) {
            setRollbackOnly(tx);
            throw EjbLogger.ROOT_LOGGER.unexpectedErrorRolledBack(e);
        } catch (Exception e) {
            ApplicationExceptionDetails ae = component.getApplicationException(e.getClass(), invocation.getMethod());

            if (ae != null) {
                if (ae.isRollback()) setRollbackOnly(tx);
                throw e;
            }
            try {
                throw e;
            } catch (EJBTransactionRolledbackException | NoSuchEJBException | NoSuchEntityException e2) {
                setRollbackOnly(tx);
                throw e2;
            } catch (RuntimeException e2) {
                setRollbackOnly(tx);
                throw new EJBTransactionRolledbackException(e2.getMessage(), e2);
            }
        } catch (Throwable t) {
            throw new EJBException(new UndeclaredThrowableException(t));
        }
    }

    protected Object invokeInNoTx(InterceptorContext invocation, final EJBComponent component) throws Exception {
        try {
            return invocation.proceed();
        } catch (Error e) {
            throw EjbLogger.ROOT_LOGGER.unexpectedError(e);
        } catch (EJBException e) {
            throw e;
        } catch (RuntimeException e) {
            ApplicationExceptionDetails ae = component.getApplicationException(e.getClass(), invocation.getMethod());
            throw ae != null ? e : new EJBException(e);
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            throw new EJBException(new UndeclaredThrowableException(t));
        }
    }

    protected Object invokeInOurTx(InterceptorContext invocation, final EJBComponent component) throws Exception {
        Transaction tx = beginTransaction();
        try {
            return invocation.proceed();
        } catch (Throwable t) {
            throw handleExceptionInOurTx(invocation, t, tx, component);
        } finally {
            endTransaction(tx);
        }
    }

    protected Transaction beginTransaction() throws NotSupportedException, SystemException {
        final ContextTransactionManager tm = ContextTransactionManager.getInstance();
        tm.begin();
        return tm.getTransaction();
    }

    protected Object mandatory(InterceptorContext invocation, final EJBComponent component) throws Exception {
        final ContextTransactionManager tm = ContextTransactionManager.getInstance();
        Transaction tx = tm.getTransaction();
        if (tx == null) {
            throw EjbLogger.ROOT_LOGGER.txRequiredForInvocation(invocation);
        }
        return invokeInCallerTx(invocation, tx, component);
    }

    protected Object never(InterceptorContext invocation, final EJBComponent component) throws Exception {
        final ContextTransactionManager tm = ContextTransactionManager.getInstance();
        if (tm.getTransaction() != null) {
            throw EjbLogger.ROOT_LOGGER.txPresentForNeverTxAttribute();
        }
        return invokeInNoTx(invocation, component);
    }

    protected Object notSupported(InterceptorContext invocation, final EJBComponent component) throws Exception {
        final ContextTransactionManager tm = ContextTransactionManager.getInstance();
        Transaction tx = tm.getTransaction();
        if (tx != null) {
            tm.suspend();
            try {
                return invokeInNoTx(invocation, component);
            } finally {
                tm.resume(tx);
            }
        } else {
            return invokeInNoTx(invocation, component);
        }
    }

    protected Object required(final InterceptorContext invocation, final EJBComponent component, final int timeout) throws Exception {
        final ContextTransactionManager tm = ContextTransactionManager.getInstance();

        if (timeout != -1) {
            tm.setTransactionTimeout(timeout);
        }

        final Transaction tx = tm.getTransaction();

        if (tx == null) {
            return invokeInOurTx(invocation, component);
        } else {
            return invokeInCallerTx(invocation, tx, component);
        }
    }

    protected Object requiresNew(InterceptorContext invocation, final EJBComponent component, final int timeout) throws Exception {
        final ContextTransactionManager tm = ContextTransactionManager.getInstance();

        if (timeout != -1) {
            tm.setTransactionTimeout(timeout);
        }

        Transaction tx = tm.getTransaction();
        if (tx != null) {
            tm.suspend();
            try {
                return invokeInOurTx(invocation, component);
            } finally {
                tm.resume(tx);
            }
        } else {
            return invokeInOurTx(invocation, component);
        }
    }

    /**
     * The <code>setRollbackOnly</code> method calls setRollbackOnly()
     * on the invocation's transaction and logs any exceptions than may
     * occur.
     *
     * @param tx the transaction
     */
    protected void setRollbackOnly(Transaction tx) {
        try {
            tx.setRollbackOnly();
        } catch (SystemException ex) {
            EjbLogger.ROOT_LOGGER.failedToSetRollbackOnly(ex);
        } catch (IllegalStateException ex) {
            EjbLogger.ROOT_LOGGER.failedToSetRollbackOnly(ex);
        }
    }

    protected Object supports(InterceptorContext invocation, final EJBComponent component) throws Exception {
        final ContextTransactionManager tm = ContextTransactionManager.getInstance();
        Transaction tx = tm.getTransaction();
        if (tx == null) {
            return invokeInNoTx(invocation, component);
        } else {
            return invokeInCallerTx(invocation, tx, component);
        }
    }
}
