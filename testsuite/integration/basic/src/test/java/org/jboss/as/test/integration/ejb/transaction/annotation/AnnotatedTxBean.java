package org.jboss.as.test.integration.ejb.transaction.annotation;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

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
