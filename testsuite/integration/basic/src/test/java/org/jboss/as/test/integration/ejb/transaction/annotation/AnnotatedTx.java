package org.jboss.as.test.integration.ejb.transaction.annotation;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

public interface AnnotatedTx {
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public int getActiveTransaction();

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public int getNonActiveTransaction();
}
