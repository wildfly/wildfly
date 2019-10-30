package org.jboss.as.test.iiop.transaction;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.RemoteHome;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPTransactionalStatefulHome.class)
@Stateful
public class IIOPTransactionalStatefulBean implements SessionSynchronization {

    private Boolean commitSucceeded;
    private boolean beforeCompletion = false;
    private Object transactionKey = null;
    private boolean rollbackOnlyBeforeCompletion = false;

    @Resource
    private SessionContext sessionContext;

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public void ejbCreate() {

    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() {
        return transactionSynchronizationRegistry.getTransactionStatus();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void resetStatus() {
        commitSucceeded = null;
        beforeCompletion = false;
        transactionKey = null;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void setRollbackOnlyBeforeCompletion(boolean rollbackOnlyBeforeCompletion) throws RemoteException {
        this.rollbackOnlyBeforeCompletion = rollbackOnlyBeforeCompletion;
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void sameTransaction(boolean first) throws RemoteException {
        if (first) {
            transactionKey = transactionSynchronizationRegistry.getTransactionKey();
        } else {
            if (!transactionKey.equals(transactionSynchronizationRegistry.getTransactionKey())) {
                throw new RemoteException("Transaction on second call was not the same as on first call");
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void rollbackOnly() throws RemoteException {
        sessionContext.setRollbackOnly();
    }

    @Override
    public void afterBegin() throws EJBException, RemoteException {

    }

    @Override
    public void beforeCompletion() throws EJBException, RemoteException {
        beforeCompletion = true;

        if (rollbackOnlyBeforeCompletion) {
            sessionContext.setRollbackOnly();
        }
    }

    @Override
    public void afterCompletion(final boolean committed) throws EJBException, RemoteException {
        commitSucceeded = committed;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Boolean getCommitSucceeded() {
        return commitSucceeded;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isBeforeCompletion() {
        return beforeCompletion;
    }
}
