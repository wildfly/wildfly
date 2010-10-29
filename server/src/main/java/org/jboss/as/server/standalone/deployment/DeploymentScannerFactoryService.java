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

package org.jboss.as.server.standalone.deployment;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.as.deployment.scanner.DeploymentScanner;
import org.jboss.as.deployment.scanner.DeploymentScannerFactory;
import org.jboss.as.model.ServerModel;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentManager;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * DeploymentScanner factory service.
 *
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerFactoryService implements DeploymentScannerFactory, Service<DeploymentScannerFactory> {

    private final InjectedValue<ScheduledExecutorService> injectedScheduleExecutor = new InjectedValue<ScheduledExecutorService>();
    private final InjectedValue<ServerDeploymentManager> injectedDeploymentManager = new InjectedValue<ServerDeploymentManager>();
    private final InjectedValue<ServerModel> injectedServerModel = new InjectedValue<ServerModel>();

    private ScheduledExecutorService executorService;
    private ServerDeploymentManager deploymentMgr;
    private ServerModel model;

    public static BatchServiceBuilder<?> addService(final BatchBuilder builder) {
        final DeploymentScannerFactoryService service = new DeploymentScannerFactoryService();
        // FIXME inject ScheduledExecutorService from an external service dependency
        final ScheduledExecutorService hack = Executors.newSingleThreadScheduledExecutor();

        service.injectedScheduleExecutor.inject(hack);
        return builder.addService(DeploymentScannerFactory.SERVICE_NAME, service)
                .addDependency(ServerDeploymentManager.SERVICE_NAME_LOCAL, ServerDeploymentManager.class, service.injectedDeploymentManager)
                .addDependency(ServerModel.SERVICE_NAME, ServerModel.class, service.injectedServerModel)
                .setInitialMode(Mode.ON_DEMAND);
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        this.model = injectedServerModel.getValue();
        this.executorService = injectedScheduleExecutor.getValue();
        this.deploymentMgr = injectedDeploymentManager.getValue();
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        this.model = null;
        this.executorService = null;
        this.deploymentMgr = null;
    }

    /** {@inheritDoc} */
    public synchronized DeploymentScanner create(String path, long scanInterval) {
        final File deploymentDir = new File(path);
        final FileSystemDeploymentService scanner = new FileSystemDeploymentService(deploymentDir, scanInterval);
        scanner.setScheduledExecutor(executorService);
        scanner.setDeploymentManager(deploymentMgr);
        scanner.setServerModel(model);
        scanner.validateAndCreate();
        return scanner;
    }

    /** {@inheritDoc} */
    public synchronized DeploymentScannerFactory getValue() throws IllegalStateException {
        return this;
    }

}
