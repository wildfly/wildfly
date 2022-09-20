package org.jboss.as.test.iiop.transaction;

import jakarta.annotation.Resource;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPTransactionalHome.class)
@Stateless
public class IIOPTransactionalStatelessBean {

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() {
        return transactionSynchronizationRegistry.getTransactionStatus();
    }

}
