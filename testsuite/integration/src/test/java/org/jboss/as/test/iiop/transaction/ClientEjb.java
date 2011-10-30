package org.jboss.as.test.iiop.transaction;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import junit.framework.Assert;

/**
 * @author Stuart Douglas
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ClientEjb {

    @Resource
    private UserTransaction userTransaction;

    @EJB(lookup = "corbaname:iiop:localhost:3628#server/server/IIOPTransactionalBMTBean")
    private IIOPTransactionalHome home;

    public void basicTransactionPropagationTest() throws RemoteException, SystemException, NotSupportedException {

        final IIOPTransactionalRemote remote = home.create();
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, remote.transactionStatus());
        userTransaction.begin();
        try {
            Assert.assertEquals(Status.STATUS_ACTIVE, remote.transactionStatus());
        } finally {
            userTransaction.rollback();
        }

    }


}
