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

package org.jboss.as.deployment.scanner;

import java.util.concurrent.TimeUnit;

import org.jboss.as.services.path.AbsolutePathService;
import org.jboss.as.services.path.RelativePathService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.ServiceController.Mode;
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
    private final InjectedValue<DeploymentScannerFactory> scannerFactory = new InjectedValue<DeploymentScannerFactory>();

    public static ServiceName getServiceName(String repositoryName) {
        return DeploymentScanner.BASE_SERVICE_NAME.append(repositoryName);
    }

    /**
     * Add the deployment scanner service to a batch.
     *
     * @param batchBuilder the batch builder
     * @param name the repository name
     * @param relativeTo the relative to
     * @param path the path
     * @param scanInterval the scan interval
     * @param timeUnit the time unit
     * @param scanEnabled scan enabled
     * @return
     */
    public static BatchServiceBuilder<DeploymentScanner> addService(final BatchBuilder batchBuilder,
            final String name, final String relativeTo, final String path, final int scanInterval, TimeUnit unit, final boolean scanEnabled) {
        final DeploymentScannerService service = new DeploymentScannerService(scanInterval, unit, scanEnabled);
        final ServiceName serviceName = getServiceName(name);
        final ServiceName pathService = serviceName.append("path");

        if(relativeTo != null) {
            RelativePathService.addService(pathService, path, relativeTo, batchBuilder);
        } else {
            AbsolutePathService.addService(pathService, path, batchBuilder);
        }

        return batchBuilder.addService(serviceName, service)
            .addDependency(pathService, String.class, service.pathValue)
            .addDependency(DeploymentScannerFactory.SERVICE_NAME, DeploymentScannerFactory.class, service.scannerFactory)
            .setInitialMode(Mode.ACTIVE);

    }

    DeploymentScannerService(final long interval, final TimeUnit unit, final boolean enabled) {
        this.interval = interval;
        this.unit = unit;
        this.enabled = enabled;
    }


    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        try {
            final String pathName = pathValue.getValue();
            final DeploymentScannerFactory factory = scannerFactory.getValue();
            final DeploymentScanner scanner = factory.create(pathName, unit.toMillis(interval));
            if(enabled) {
                scanner.startScanner();
            }
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        final DeploymentScanner scanner = this.scanner;
        this.scanner = null;
        scanner.stopScanner();
    }

    /** {@inheritDoc} */
    public synchronized DeploymentScanner getValue() throws IllegalStateException {
        final DeploymentScanner scanner = this.scanner;
        if(scanner == null) {
            throw new IllegalStateException();
        }
        return scanner;
    }

}
