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

package org.jboss.as.server.deployment.scanner;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jboss.as.server.NewServerController;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible creating a {@code DeploymentScanner}
 *
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerService implements Service<DeploymentScanner> {

    private long interval;
    private TimeUnit unit = TimeUnit.MILLISECONDS;
    private boolean enabled;

    /** The created scanner. */
    private DeploymentScanner scanner;

    private final InjectedValue<String> pathValue = new InjectedValue<String>();
    private final InjectedValue<NewServerController> serverControllerValue = new InjectedValue<NewServerController>();
    private final InjectedValue<DeploymentRepository> deploymentRepositoryValue = new InjectedValue<DeploymentRepository>();
    private final InjectedValue<ScheduledExecutorService> scheduledExecutorValue = new InjectedValue<ScheduledExecutorService>();

    public static ServiceName getServiceName(String repositoryName) {
        return DeploymentScanner.BASE_SERVICE_NAME.append(repositoryName);
    }

    /**
     * Add the deployment scanner service to a batch.
     *
     * @param serviceTarget the service target
     * @param name the repository name
     * @param relativeTo the relative to
     * @param path the path
     * @param scanInterval the scan interval
     * @param scanEnabled scan enabled
     * @return
     */
    public static void addService(final ServiceTarget serviceTarget, final String name, final String relativeTo, final String path, final int scanInterval, TimeUnit unit, final boolean scanEnabled) {
        final DeploymentScannerService service = new DeploymentScannerService(scanInterval, unit, scanEnabled);
        final ServiceName serviceName = getServiceName(name);
        final ServiceName pathService = serviceName.append("path");

        if(relativeTo != null) {
            RelativePathService.addService(pathService, path, relativeTo, serviceTarget);
        } else {
            AbsolutePathService.addService(pathService, path, serviceTarget);
        }

        final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

        serviceTarget.addService(serviceName, service)
            .addDependency(pathService, String.class, service.pathValue)
            .addDependency(Services.JBOSS_SERVER_CONTROLLER, NewServerController.class, service.serverControllerValue)
            .addDependency(ServerDeploymentRepository.SERVICE_NAME, DeploymentRepository.class, service.deploymentRepositoryValue)
            .addInjection(service.scheduledExecutorValue, scheduledExecutorService)
            .setInitialMode(Mode.ACTIVE)
            .install();
    }

    DeploymentScannerService(final long interval, final TimeUnit unit, final boolean enabled) {
        this.interval = interval;
        this.unit = unit;
        this.enabled = enabled;
    }


    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            final String pathName = pathValue.getValue();

            final FileSystemDeploymentService scanner = new FileSystemDeploymentService(new File(pathName), unit.toMillis(interval), serverControllerValue.getValue(), scheduledExecutorValue.getValue(), deploymentRepositoryValue.getValue());

            if(enabled) {
                scanner.startScanner();
            }
            this.scanner = scanner;
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        final DeploymentScanner scanner = this.scanner;
        this.scanner = null;
        scanner.stopScanner();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized DeploymentScanner getValue() throws IllegalStateException {
        final DeploymentScanner scanner = this.scanner;
        if(scanner == null) {
            throw new IllegalStateException();
        }
        return scanner;
    }

}
