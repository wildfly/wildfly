package org.jboss.as.test.integration.ejb.transaction.annotation;

import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

public interface AnnotatedTx {
    @TransactionAttribute(TransactionAttributeType.NEVER)
    int getActiveTransaction();

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    int getNonActiveTransaction();
}
