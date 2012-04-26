package org.jboss.as.test.integration.ejb.stateful.locking;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

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
