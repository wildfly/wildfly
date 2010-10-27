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

package org.jboss.as.connector.bootstrap;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.tm.JBossXATerminator;

/**
 * A DefaultBootStrapContextService Service
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public final class DefaultBootStrapContextService implements Service<CloneableBootstrapContext> {

    private final CloneableBootstrapContext value;

    private final InjectedValue<WorkManager> workManagerValue = new InjectedValue<WorkManager>();

    private final InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> txManager = new InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService>();

    private final InjectedValue<JBossXATerminator> xaTerminator = new InjectedValue<JBossXATerminator>();

    public Value<WorkManager> getWorkManagerValue() {
        return workManagerValue;
    }

    private static final Logger log = Logger.getLogger("org.jboss.as.connector");

    /** create an instance **/
    public DefaultBootStrapContextService(CloneableBootstrapContext value) {
        super();
        log.debugf("Building DefaultBootstrapContext");
        this.value = value;

    }

    @Override
    public CloneableBootstrapContext getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.value.setWorkManager(workManagerValue.getValue());
        this.value.setTransactionSynchronizationRegistry(txManager.getValue().getTransactionSynchronizationRegistry());
        this.value.setXATerminator(xaTerminator.getValue());
        log.debugf("Starting JCA DefaultBootstrapContext");
    }

    @Override
    public void stop(StopContext context) {

    }

    public Injector<com.arjuna.ats.jbossatx.jta.TransactionManagerService> getTxManagerInjector() {
        return txManager;
    }

    public Injector<JBossXATerminator> getXaTerminatorInjector() {
        return xaTerminator;
    }

    public Injector<WorkManager> getWorkManagerValueInjector() {
        return workManagerValue;
    }

}
