package org.jboss.as.test.integration.weld.jta;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.TransactionSynchronizationRegistry;

@ApplicationScoped
public class CdiBean {

    @Resource
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public boolean isTransactionSynchronizationRegistryInjected() {
        return transactionSynchronizationRegistry != null;
    }

}
