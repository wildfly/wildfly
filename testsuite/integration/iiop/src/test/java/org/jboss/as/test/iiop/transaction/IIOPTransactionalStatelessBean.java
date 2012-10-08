package org.jboss.as.test.iiop.transaction;

import javax.annotation.Resource;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionSynchronizationRegistry;

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
