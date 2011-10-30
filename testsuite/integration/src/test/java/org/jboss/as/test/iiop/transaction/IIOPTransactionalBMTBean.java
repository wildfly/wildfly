package org.jboss.as.test.iiop.transaction;

import javax.annotation.Resource;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPTransactionalHome.class)
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class IIOPTransactionalBMTBean {

    @Resource
    private UserTransaction userTransaction;


    public int transactionStatus() {
        try {
            return userTransaction.getStatus();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

}
