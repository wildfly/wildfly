package org.jboss.as.messaging.jms;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

import javax.transaction.TransactionManager;

/**
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 *         Date: 5/16/11
 *         Time: 3:47 PM
 */
public class TransactionManagerLocatorService {

    private InjectedValue<TransactionManager> transactionManager = new InjectedValue<TransactionManager>();


    public Injector<TransactionManager> getInjectedTransactionManager() {
        return transactionManager;
    }


    public TransactionManager getTransactionManager() {
        return transactionManager.getValue();
    }
}
