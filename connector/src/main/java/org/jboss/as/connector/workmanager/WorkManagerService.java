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

package org.jboss.as.connector.workmanager;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.jca.core.security.UsersRoles;
import org.jboss.jca.core.spi.security.Callback;
import org.jboss.jca.core.tx.jbossts.XATerminatorImpl;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.BlockingExecutor;
import org.jboss.tm.JBossXATerminator;

import java.util.concurrent.Executor;

import static org.jboss.as.connector.ConnectorLogger.ROOT_LOGGER;

/**
 * A WorkManager Service.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class WorkManagerService implements Service<WorkManager> {

    private final WorkManager value;

    private final InjectedValue<Executor> executorShort = new InjectedValue<Executor>();

    private final InjectedValue<Executor> executorLong = new InjectedValue<Executor>();

    private final InjectedValue<JBossXATerminator> xaTerminator = new InjectedValue<JBossXATerminator>();

    private volatile Callback callback;

    /** create an instance **/
    public WorkManagerService(WorkManager value) {
        super();
        ROOT_LOGGER.debugf("Building WorkManager");
        this.value = value;

    }

    @Override
    public WorkManager getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        BlockingExecutor longRunning = (BlockingExecutor) executorLong.getOptionalValue();
        if (longRunning != null) {
            this.value.setLongRunningThreadPool(longRunning);
            this.value.setShortRunningThreadPool((BlockingExecutor) executorShort.getValue());
        } else {
            this.value.setLongRunningThreadPool((BlockingExecutor) executorShort.getValue());
            this.value.setShortRunningThreadPool((BlockingExecutor) executorShort.getValue());

        }
        this.value.setXATerminator(new XATerminatorImpl(xaTerminator.getValue()));

        // TODO - Remove and do proper integration
        String usersProperties = System.getProperty("users.properties");
        String rolesProperties = System.getProperty("roles.properties");

        if (usersProperties != null && rolesProperties != null) {
            try {
                UsersRoles usersRoles = new UsersRoles();
                usersRoles.setUsersProperties(usersProperties);
                usersRoles.setRolesProperties(rolesProperties);
                usersRoles.start();

                this.callback = usersRoles;
                this.value.setCallbackSecurity(callback);
            } catch (Throwable t) {
                ROOT_LOGGER.debug(t.getMessage(), t);
            }
        }

        ROOT_LOGGER.debugf("Starting JCA WorkManager");
    }

    @Override
    public void stop(StopContext context) {
        value.shutdown();

        try {
            if (callback != null)
                callback.stop();
        } catch (Throwable t) {
            ROOT_LOGGER.debug(t.getMessage(), t);
        }
    }

    public Injector<Executor> getExecutorShortInjector() {
        return executorShort;
    }

    public Injector<Executor> getExecutorLongInjector() {
        return executorLong;
    }

    public Injector<JBossXATerminator> getXaTerminatorInjector() {
        return xaTerminator;
    }

}
