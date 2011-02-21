/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.TransactionalProxyController;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
public final class DomainControllerService implements Service<DomainController> {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private final ExtensibleConfigurationPersister configurationPersister;
    private final InjectedValue<ScheduledExecutorService> scheduledExecutorService = new InjectedValue<ScheduledExecutorService>();
    private final InjectedValue<MasterDomainControllerClient> masterDomainControllerClient = new InjectedValue<MasterDomainControllerClient>();
    private final InjectedValue<TransactionalProxyController> hostController = new InjectedValue<TransactionalProxyController>();
    private final String localHostName;
    private DomainController controller;

    public DomainControllerService(final ExtensibleConfigurationPersister configurationPersister, final String localHostName) {
        this.configurationPersister = configurationPersister;
        this.localHostName = localHostName;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(final StartContext context) throws StartException {
        MasterDomainControllerClient masterClient = masterDomainControllerClient.getOptionalValue();
        this.controller = masterClient == null ? startMasterDomainController() : startSlaveDomainController(masterClient);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(final StopContext context) {
        this.controller = null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized DomainController getValue() throws IllegalStateException, IllegalArgumentException {
        final DomainController controller = this.controller;
        if(controller == null) {
            throw new IllegalStateException();
        }
        return controller;
    }

    public Injector<ScheduledExecutorService> getScheduledExecutorServiceInjector() {
        return scheduledExecutorService;
    }

    public Injector<TransactionalProxyController> getHostControllerServiceInjector() {
        return hostController;
    }

    public Injector<MasterDomainControllerClient> getMasterDomainControllerClientInjector() {
        return masterDomainControllerClient;
    }

    private DomainController startMasterDomainController() throws StartException {

        log.info("Starting Domain Controller");
        DomainModelImpl domainModel = new DomainModelImpl(configurationPersister, hostController.getValue());
        final List<ModelNode> updates;
        try {
             updates = configurationPersister.load();
        } catch (final Exception e) {
            log.error("failed to start domain controller", e);
            throw new StartException(e);
        }

        final AtomicInteger count = new AtomicInteger(1);
        final ResultHandler resultHandler = new ResultHandler() {
            @Override
            public void handleResultFragment(final String[] location, final ModelNode result) {
            }

            @Override
            public void handleResultComplete() {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }

            @Override
            public void handleFailed(final ModelNode failureDescription) {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }

            @Override
            public void handleCancellation() {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }
        };
        for (ModelNode update : updates) {
            count.incrementAndGet();
            domainModel.execute(update, resultHandler);
        }
        if (count.decrementAndGet() == 0) {
            // some action?
        }

        return new DomainControllerImpl(scheduledExecutorService.getValue(), domainModel, localHostName);
    }

    private DomainController startSlaveDomainController(MasterDomainControllerClient masterClient) {
        log.info("Starting Domain Controller Slave");
        DomainModelImpl domainModel = new DomainModelImpl(new ModelNode(), configurationPersister, hostController.getValue());
        controller = new DomainControllerImpl(scheduledExecutorService.getValue(), domainModel, localHostName, masterClient);
        return controller;
    }


}
