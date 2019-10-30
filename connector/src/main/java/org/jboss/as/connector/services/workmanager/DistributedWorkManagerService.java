/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.connector.services.workmanager;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import java.util.concurrent.Executor;

import org.jboss.as.connector.security.ElytronSecurityIntegration;
import org.jboss.as.connector.services.workmanager.transport.CommandDispatcherTransport;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.jca.core.security.picketbox.PicketBoxSecurityIntegration;
import org.jboss.jca.core.spi.workmanager.Address;
import org.jboss.jca.core.tx.jbossts.XATerminatorImpl;
import org.jboss.jca.core.workmanager.WorkManagerCoordinator;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.BlockingExecutor;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;

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
        ROOT_LOGGER.debugf("Starting JCA DistributedWorkManager: ", value.getName());

        CommandDispatcherTransport transport = new CommandDispatcherTransport(this.dispatcherFactory.getValue(), this.value.getName());

        this.value.setTransport(transport);

        BlockingExecutor longRunning = (BlockingExecutor) executorLong.getOptionalValue();
        if (longRunning != null) {
            this.value.setLongRunningThreadPool(longRunning);
            this.value.setShortRunningThreadPool(new StatisticsExecutorImpl((BlockingExecutor) executorShort.getValue()));
        } else {
            this.value.setLongRunningThreadPool(new StatisticsExecutorImpl((BlockingExecutor) executorShort.getValue()));
            this.value.setShortRunningThreadPool(new StatisticsExecutorImpl((BlockingExecutor) executorShort.getValue()));

        }

        this.value.setXATerminator(new XATerminatorImpl(xaTerminator.getValue()));


        if (this.value.isElytronEnabled()) {
            this.value.setSecurityIntegration(new ElytronSecurityIntegration());
        } else {
            this.value.setSecurityIntegration(new PicketBoxSecurityIntegration());
        }

        try {
            transport.startup();
        } catch (Throwable throwable) {
            ROOT_LOGGER.trace("failed to start DWM transport:", throwable);
            throw ROOT_LOGGER.failedToStartDWMTransport(this.value.getName());
        }
        transport.register(new Address(value.getId(), value.getName(), transport.getId()));

        WorkManagerCoordinator.getInstance().registerWorkManager(value);


        ROOT_LOGGER.debugf("Started JCA DistributedWorkManager: ", value.getName());
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopping JCA DistributedWorkManager: ", value.getName());

        value.prepareShutdown();

        try {
            value.getTransport().shutdown();
        } catch (Throwable throwable) {
            ROOT_LOGGER.trace("failed to stop DWM transport:", throwable);

        }

        value.shutdown();

        WorkManagerCoordinator.getInstance().unregisterWorkManager(value);

        ROOT_LOGGER.debugf("Stopped JCA DistributedWorkManager: ", value.getName());
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
