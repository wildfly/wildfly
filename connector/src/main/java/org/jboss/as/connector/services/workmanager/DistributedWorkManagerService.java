/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import java.util.concurrent.Executor;

import org.jboss.as.connector.security.ElytronSecurityIntegration;
import org.jboss.as.connector.services.workmanager.transport.CommandDispatcherTransport;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.jca.core.spi.workmanager.Address;
import org.jboss.jca.core.tx.jbossts.XATerminatorImpl;
import org.jboss.jca.core.workmanager.WorkManagerCoordinator;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;

/**
 * A WorkManager Service.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class DistributedWorkManagerService implements Service<NamedDistributedWorkManager> {

    private final NamedDistributedWorkManager value;

    private final InjectedValue<Executor> executorShort = new InjectedValue<Executor>();

    private final InjectedValue<Executor> executorLong = new InjectedValue<Executor>();

    private final InjectedValue<JBossContextXATerminator> xaTerminator = new InjectedValue<JBossContextXATerminator>();

    private final InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();

    /**
     * create an instance
     *
     * @param value the work manager
     */
    public DistributedWorkManagerService(NamedDistributedWorkManager value) {
        super();
        ROOT_LOGGER.debugf("Building DistributedWorkManager");
        this.value = value;
    }

    @Override
    public NamedDistributedWorkManager getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting Jakarta Connectors DistributedWorkManager: ", value.getName());

        CommandDispatcherTransport transport = new CommandDispatcherTransport(this.dispatcherFactory.getValue(), this.value.getName());

        this.value.setTransport(transport);

        Executor longRunning = executorLong.getOptionalValue();
        if (longRunning != null) {
            this.value.setLongRunningThreadPool(new StatisticsExecutorImpl(longRunning));
            this.value.setShortRunningThreadPool(new StatisticsExecutorImpl(executorShort.getValue()));
        } else {
            this.value.setLongRunningThreadPool(new StatisticsExecutorImpl(executorShort.getValue()));
            this.value.setShortRunningThreadPool(new StatisticsExecutorImpl(executorShort.getValue()));

        }

        this.value.setXATerminator(new XATerminatorImpl(xaTerminator.getValue()));

        this.value.setSecurityIntegration(new ElytronSecurityIntegration());

        try {
            transport.startup();
        } catch (Throwable throwable) {
            ROOT_LOGGER.trace("failed to start DWM transport:", throwable);
            throw ROOT_LOGGER.failedToStartDWMTransport(this.value.getName());
        }
        transport.register(new Address(value.getId(), value.getName(), transport.getId()));

        WorkManagerCoordinator.getInstance().registerWorkManager(value);


        ROOT_LOGGER.debugf("Started Jakarta Connectors DistributedWorkManager: ", value.getName());
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopping Jakarta Connectors DistributedWorkManager: ", value.getName());

        value.prepareShutdown();

        try {
            value.getTransport().shutdown();
        } catch (Throwable throwable) {
            ROOT_LOGGER.trace("failed to stop DWM transport:", throwable);

        }

        value.shutdown();

        WorkManagerCoordinator.getInstance().unregisterWorkManager(value);

        ROOT_LOGGER.debugf("Stopped Jakarta Connectors DistributedWorkManager: ", value.getName());
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

    public Injector<CommandDispatcherFactory> getCommandDispatcherFactoryInjector() {
        return this.dispatcherFactory;
    }
}
