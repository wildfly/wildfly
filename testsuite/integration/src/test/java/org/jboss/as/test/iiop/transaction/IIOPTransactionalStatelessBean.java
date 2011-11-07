package org.jboss.as.test.iiop.transaction;

import javax.annotation.Resource;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPTransactionalHome.class)
@Stateless
public class IIOPTransactionalStatelessBean {

    @Resource
    private UserTransaction userTransaction;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() {
        try {
            return userTransaction.getStatus();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

}
