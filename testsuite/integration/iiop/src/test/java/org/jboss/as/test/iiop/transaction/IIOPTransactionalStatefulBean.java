package org.jboss.as.test.iiop.transaction;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.RemoteHome;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPTransactionalStatefulHome.class)
@Stateful
public class IIOPTransactionalStatefulBean implements SessionSynchronization {

    private Boolean commitSuceeded;
    private boolean beforeCompletion = false;
    private Object transactionKey = null;
    private boolean rollbackOnlyBeforeCompletion = false;

    @Resource
    private UserTransaction userTransaction;

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public void ejbCreate() {

    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() {
        try {
            return userTransaction.getStatus();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void resetStatus() {
        commitSuceeded = null;
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
        try {
            userTransaction.setRollbackOnly();
        } catch (SystemException e) {
            throw new RemoteException("SystemException during setRollbackOnly", e);
        }
    }

    @Override
    public void afterBegin() throws EJBException, RemoteException {

    }

    @Override
    public void beforeCompletion() throws EJBException, RemoteException {
        beforeCompletion = true;

        if (rollbackOnlyBeforeCompletion) {
            try {
                userTransaction.setRollbackOnly();
            } catch (SystemException e) {
                throw new RemoteException("SystemException during setRollbackOnly", e);
            }
        }
    }

    @Override
    public void afterCompletion(final boolean committed) throws EJBException, RemoteException {
        commitSuceeded = committed;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Boolean getCommitSuceeded() {
        return commitSuceeded;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isBeforeCompletion() {
        return beforeCompletion;
    }
}
