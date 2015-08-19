package org.jboss.as.security;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.plugins.TransactionManagerLocator;

import javax.transaction.TransactionManager;

/**
 * Service that initializes the TransactionManagerLocator.
 *
 * Note that even if the transaction manager is not present this service will still be installed,
 * so services can depend on it without needing to do a check for the capability.
 *
 * @author Stuart Douglas
 */
public class TransactionManagerLocatorService implements Service<Void> {

    public static ServiceName SERVICE_NAME = SecurityExtension.JBOSS_SECURITY.append("transaction-manager-locator");

    private final InjectedValue<TransactionManager> transactionManagerInjectedValue = new InjectedValue<>();

    @Override
    public void start(StartContext startContext) throws StartException {
        TransactionManagerLocator.setTransactionManager(transactionManagerInjectedValue.getValue());
    }

    @Override
    public void stop(StopContext stopContext) {
        TransactionManagerLocator.setTransactionManager(null);
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    public InjectedValue<TransactionManager> getTransactionManagerInjectedValue() {
        return transactionManagerInjectedValue;
    }
}
