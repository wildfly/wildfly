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
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;

import java.util.concurrent.Executor;

import org.jboss.as.connector.security.ElytronSecurityIntegration;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.jca.core.security.picketbox.PicketBoxSecurityIntegration;
import org.jboss.jca.core.tx.jbossts.XATerminatorImpl;
import org.jboss.jca.core.workmanager.WorkManagerCoordinator;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.BlockingExecutor;

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
        ROOT_LOGGER.debugf("Starting JCA WorkManager: ", value.getName());

        BlockingExecutor longRunning = (BlockingExecutor) executorLong.getOptionalValue();
        if (longRunning != null) {
            this.value.setLongRunningThreadPool(longRunning);
            this.value.setShortRunningThreadPool(new StatisticsExecutorImpl((BlockingExecutor) executorShort.getValue()));
        } else {
            this.value.setLongRunningThreadPool(new StatisticsExecutorImpl((BlockingExecutor) executorShort.getValue()));
            this.value.setShortRunningThreadPool(new StatisticsExecutorImpl((BlockingExecutor) executorShort.getValue()));

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


        if (this.value.isElytronEnabled()) {
            this.value.setSecurityIntegration(new ElytronSecurityIntegration());
        } else {
            this.value.setSecurityIntegration(new PicketBoxSecurityIntegration());
        }
        ROOT_LOGGER.debugf("Started JCA WorkManager: ", value.getName());
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopping JCA WorkManager: ", value.getName());

        //shutting down immediately (synchronous method) the workmanager and release all works
        value.shutdown();

        if (value.getName().equals(DEFAULT_NAME)) {
            WorkManagerCoordinator.getInstance().setDefaultWorkManager(null);
        } else {
            WorkManagerCoordinator.getInstance().unregisterWorkManager(value);
        }

        ROOT_LOGGER.debugf("Stopped JCA WorkManager: ", value.getName());
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
