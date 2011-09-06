package org.jboss.as.connector.services;

import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.connector.ConnectorLogger.ROOT_LOGGER;

public class CcmService implements Service<CachedConnectionManager> {

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("connector", "admin-object");

    private final InjectedValue<TransactionIntegration> transactionIntegration = new InjectedValue<TransactionIntegration>();

    private volatile CachedConnectionManager value;

    private final boolean debug;
    private final boolean error;

    /** create an instance **/
    public CcmService(final boolean debug, final boolean error) {
        super();
        this.debug = debug;
        this.error = error;
    }

    @Override
    public CachedConnectionManager getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        value = new CachedConnectionManagerImpl(transactionIntegration.getValue().getTransactionManager(),
                transactionIntegration.getValue().getTransactionSynchronizationRegistry());
        value.setDebug(debug);
        value.setError(error);
        ROOT_LOGGER.debugf("started CcmService %s", context.getController().getName());

    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("stopped CcmService %s", context.getController().getName());

    }

    public Injector<TransactionIntegration> getTransactionIntegrationInjector() {
        return transactionIntegration;
    }

}
