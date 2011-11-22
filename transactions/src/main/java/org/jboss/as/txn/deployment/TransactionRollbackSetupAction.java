package org.jboss.as.txn.deployment;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.txn.TransactionLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Setup action that makes sure that no transactions leak from EE requests
 *
 * @author Stuart Douglas
 */
public class TransactionRollbackSetupAction implements SetupAction, Service<TransactionRollbackSetupAction> {


    private final InjectedValue<TransactionManager> transactionManager = new InjectedValue<TransactionManager>();

    private final ServiceName serviceName;

    public TransactionRollbackSetupAction(final ServiceName serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void setup(final Map<String, Object> properties) {

    }

    @Override
    public void teardown(final Map<String, Object> properties) {
        try {
            final TransactionManager tm = transactionManager.getValue();
            final int status = tm.getStatus();

            switch (status) {
                case Status.STATUS_ACTIVE:
                case Status.STATUS_COMMITTING:
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_PREPARING:
                case Status.STATUS_ROLLING_BACK:
                case Status.STATUS_PREPARED:
                    try {
                        TransactionLogger.ROOT_LOGGER.transactionStillOpen(status);
                        tm.rollback();
                    } catch (Exception ex) {
                        TransactionLogger.ROOT_LOGGER.unableToRollBack(ex);
                    }
            }
        } catch (Exception e) {
            TransactionLogger.ROOT_LOGGER.unableToGetTransactionStatus(e);
        }
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Set<ServiceName> dependencies() {
        return Collections.singleton(serviceName);
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public TransactionRollbackSetupAction getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<TransactionManager> getTransactionManager() {
        return transactionManager;
    }
}
