/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;

import java.util.concurrent.Executor;

import org.jboss.as.connector.security.ElytronSecurityIntegration;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.jca.core.tx.jbossts.XATerminatorImpl;
import org.jboss.jca.core.workmanager.WorkManagerCoordinator;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A WorkManager Service.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class WorkManagerService implements Service<NamedWorkManager> {

    private final NamedWorkManager value;

    private final InjectedValue<Executor> executorShort = new InjectedValue<Executor>();

    private final InjectedValue<Executor> executorLong = new InjectedValue<Executor>();

    private final InjectedValue<JBossContextXATerminator> xaTerminator = new InjectedValue<JBossContextXATerminator>();

    /**
     * create an instance
     *
     * @param value the work manager
     */
    public WorkManagerService(NamedWorkManager value) {
        super();
        ROOT_LOGGER.debugf("Building WorkManager");
        this.value = value;
    }

    @Override
    public NamedWorkManager getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting Jakarta Connectors WorkManager: ", value.getName());

        Executor longRunning = executorLong.getOptionalValue();
        if (longRunning != null) {
            this.value.setLongRunningThreadPool(longRunning);
            this.value.setShortRunningThreadPool(new StatisticsExecutorImpl(executorShort.getValue()));
        } else {
            this.value.setLongRunningThreadPool(new StatisticsExecutorImpl(executorShort.getValue()));
            this.value.setShortRunningThreadPool(new StatisticsExecutorImpl(executorShort.getValue()));

        }

        this.value.setXATerminator(new XATerminatorImpl(xaTerminator.getValue()));

        if (value.getName().equals(DEFAULT_NAME)) {
            WorkManagerCoordinator.getInstance().setDefaultWorkManager(value);
        } else {
            WorkManagerCoordinator.getInstance().registerWorkManager(value);
        }

        //this is a value.restart() equivalent
        if (value.isShutdown())
            value.cancelShutdown();

        this.value.setSecurityIntegration(new ElytronSecurityIntegration());

        ROOT_LOGGER.debugf("Started Jakarta Connectors WorkManager: ", value.getName());
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopping Jakarta Connectors WorkManager: ", value.getName());

        //shutting down immediately (synchronous method) the workmanager and release all works
        value.shutdown();

        if (value.getName().equals(DEFAULT_NAME)) {
            WorkManagerCoordinator.getInstance().setDefaultWorkManager(null);
        } else {
            WorkManagerCoordinator.getInstance().unregisterWorkManager(value);
        }

        ROOT_LOGGER.debugf("Stopped Jakarta Connectors WorkManager: ", value.getName());
    }

    public Injector<Executor> getExecutorShortInjector() {
        return executorShort;
    }

    public Injector<Executor> getExecutorLongInjector() {
        return executorLong;
    }

    public Injector<JBossContextXATerminator> getXaTerminatorInjector() {
        return xaTerminator;
    }
}
