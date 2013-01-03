package org.jboss.as.test.integration.ejb.transaction.cmt.lifecycle;

import javax.annotation.Resource;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * @author Stuart Douglas
 */
public class LifecycleSuperClass {

    @Resource
    protected TransactionSynchronizationRegistry trs;

    int state;
    Object key;

    protected void saveTxState() {
        state = trs.getTransactionStatus();
        key = trs.getTransactionKey();
    }

    public int getState() {
        return state;
    }

    public Object getKey() {
        return key;
    }
}
