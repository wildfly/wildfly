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

package org.jboss.as.connector.services.bootstrap;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.jca.core.bootstrapcontext.BootstrapContextCoordinator;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

/**
 * A DefaultBootStrapContextService Service
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public final class BootStrapContextService implements Service<CloneableBootstrapContext> {

    private final CloneableBootstrapContext value;

    private final String name;

    private final boolean usingDefaultWm;

    private final InjectedValue<WorkManager> workManagerValue = new InjectedValue<WorkManager>();

    private final InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> txManager = new InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService>();

    private final InjectedValue<JBossContextXATerminator> xaTerminator = new InjectedValue<JBossContextXATerminator>();

    private final InjectedValue<JcaSubsystemConfiguration> jcaConfig = new InjectedValue<JcaSubsystemConfiguration>();


    public Value<WorkManager> getWorkManagerValue() {
        return workManagerValue;
    }

    /** create an instance **/
    public BootStrapContextService(CloneableBootstrapContext value, String name, boolean usingDefaultWm) {
        super();
        ROOT_LOGGER.debugf("Building DefaultBootstrapContext");
        this.value = value;
        this.name = name;
        this.usingDefaultWm = usingDefaultWm;

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
        jcaConfig.getValue().getBootstrapContexts().put(name, value);
        if(usingDefaultWm) {
            jcaConfig.getValue().setDefaultBootstrapContext(value);
            BootstrapContextCoordinator.getInstance().setDefaultBootstrapContext(value);
        } else {
            BootstrapContextCoordinator.getInstance().registerBootstrapContext(value);
        }
        ROOT_LOGGER.debugf("Starting JCA DefaultBootstrapContext");
    }

    @Override
    public void stop(StopContext context) {
        jcaConfig.getValue().getBootstrapContexts().remove(name);
        if (usingDefaultWm) {
            jcaConfig.getValue().setDefaultBootstrapContext(null);
            BootstrapContextCoordinator.getInstance().setDefaultBootstrapContext(null);
        } else {
            BootstrapContextCoordinator.getInstance().unregisterBootstrapContext(value);
        }

    }

    public Injector<com.arjuna.ats.jbossatx.jta.TransactionManagerService> getTxManagerInjector() {
        return txManager;
    }

    public Injector<JBossContextXATerminator> getXaTerminatorInjector() {
        return xaTerminator;
    }

    public Injector<WorkManager> getWorkManagerValueInjector() {
        return workManagerValue;
    }

    public Injector<JcaSubsystemConfiguration> getJcaConfigInjector() {
        return jcaConfig;
    }

}
