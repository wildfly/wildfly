package org.jboss.as.test.integration.ejb.stateful.locking;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author Stuart Douglas
 */
@Stateful
public class ConcurrencySFSB {

    private int counter;

    public void addTx(int sum) {
        counter = counter + sum;
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void addNoTx(int sum) {
        counter = counter + sum;
    }

    public int getCounter() {
        return counter;
    }
}
