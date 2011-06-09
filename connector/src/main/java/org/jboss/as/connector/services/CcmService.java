package org.jboss.as.connector.services;

import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.AdminObject;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

public class CcmService implements Service<CachedConnectionManager> {

    private static final Logger log = Logger.getLogger("org.jboss.as.connector");
    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("connector", "admin-object");

    private InjectedValue<TransactionIntegration> transactionIntegration = new InjectedValue<TransactionIntegration>();

    private CachedConnectionManager value;

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
        log.debugf("started CcmService %s", context.getController().getName());

    }

    @Override
    public void stop(StopContext context) {
        log.debugf("stopped CcmService %s", context.getController().getName());

    }

    public Injector<TransactionIntegration> getTransactionIntegrationInjector() {
        return transactionIntegration;
    }

}
