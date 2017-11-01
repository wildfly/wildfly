package org.jboss.as.test.integration.ejb.transaction.annotation;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

@Stateless
public class AnnotatedTxBean implements AnnotatedTx {

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int getActiveTransaction() {
        return getTransactionStatus();
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public int getNonActiveTransaction() {
        return getTransactionStatus();
    }

    private int getTransactionStatus() {
        try {
            return transactionManager.getStatus();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}