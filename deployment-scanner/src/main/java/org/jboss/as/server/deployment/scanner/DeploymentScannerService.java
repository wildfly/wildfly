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
import java.security.AccessController;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.RelativePathService;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.JBossThreadFactory;

/**
 * Service responsible creating a {@code DeploymentScanner}
 *
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerService implements Service<DeploymentScanner> {

    private static final int DEFAULT_INTERVAL = 5000;
    private long interval;
    private TimeUnit unit = TimeUnit.MILLISECONDS;
    private boolean enabled;
    private boolean autoDeployZipped;
    private boolean autoDeployExploded;
    private boolean autoDeployXml;
    private Long deploymentTimeout;
    private final String relativeTo;

    /**
     * The created scanner.
     */
    private DeploymentScanner scanner;


    private final InjectedValue<String> relativePathValue = new InjectedValue<String>();
    private final InjectedValue<String> pathValue = new InjectedValue<String>();
    private final InjectedValue<ModelController> controllerValue = new InjectedValue<ModelController>();
    private final InjectedValue<ScheduledExecutorService> scheduledExecutorValue = new InjectedValue<ScheduledExecutorService>();

    public static ServiceName getServiceName(String repositoryName) {
        return DeploymentScanner.BASE_SERVICE_NAME.append(repositoryName);
    }

    /**
     * Add the deployment scanner service to a batch.
     *
     * @param serviceTarget     the service target
     * @param name              the repository name
     * @param relativeTo        the relative to
     * @param path              the path
     * @param scanInterval      the scan interval
     * @param scanEnabled       scan enabled
     * @param deploymentTimeout the deployment timeout
     * @return
     */
    public static ServiceController<?> addService(final ServiceTarget serviceTarget, final String name, final String relativeTo, final String path,
                                  final Integer scanInterval, TimeUnit unit, final Boolean autoDeployZip,
                                  final Boolean autoDeployExploded, final Boolean autoDeployXml, final Boolean scanEnabled, final Long deploymentTimeout,
                                  final List<ServiceController<?>> newControllers,
                                  final ServiceListener<Object>... listeners) {
        final DeploymentScannerService service = new DeploymentScannerService(relativeTo, scanInterval, unit, autoDeployZip, autoDeployExploded, autoDeployXml, scanEnabled, deploymentTimeout);
        final ServiceName serviceName = getServiceName(name);
        final ServiceName pathService = serviceName.append("path");
        final ServiceName relativePathService = relativeTo != null ? RelativePathService.pathNameOf(relativeTo) : null;

        if (relativeTo != null) {
            RelativePathService.addService(pathService, path, false, relativeTo, serviceTarget, newControllers, listeners);
        } else {
            AbsolutePathService.addService(pathService, path, serviceTarget, newControllers, listeners);
        }
        final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("DeploymentScanner-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
        final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2, threadFactory);

        ServiceBuilder builder = serviceTarget.addService(serviceName, service)
                .addDependency(pathService, String.class, service.pathValue)
                .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.controllerValue)
                .addDependency(org.jboss.as.server.deployment.Services.JBOSS_DEPLOYMENT_CHAINS)
                .addInjection(service.scheduledExecutorValue, scheduledExecutorService);
        if (relativePathService != null) {
            builder.addDependency(relativePathService, String.class, service.relativePathValue);
        }
        builder.addListener(listeners);
        ServiceController<?> svc = builder.setInitialMode(Mode.ACTIVE).install();
        if (newControllers != null) {
            newControllers.add(svc);
        }
        return svc;
    }

    DeploymentScannerService(final String relativeTo, final Integer interval, final TimeUnit unit, final Boolean autoDeployZipped,
                             final Boolean autoDeployExploded, final Boolean autoDeployXml, final Boolean enabled, final Long deploymentTimeout) {
        this.relativeTo = relativeTo;
        this.interval = interval == null ? DEFAULT_INTERVAL : interval.longValue();
        this.unit = unit;
        this.autoDeployZipped = autoDeployZipped == null ? true : autoDeployZipped.booleanValue();
        this.autoDeployExploded = autoDeployExploded == null ? false : autoDeployExploded.booleanValue();
        this.autoDeployXml = autoDeployXml == null ? true : autoDeployXml.booleanValue();
        this.enabled = enabled == null ? true : enabled.booleanValue();
        this.deploymentTimeout = deploymentTimeout;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            final String pathName = pathValue.getValue();
            final String relativePathName = relativePathValue.getOptionalValue();
            final File relativePath = relativePathName != null ? new File(relativePathName) : null;
            final FileSystemDeploymentService scanner = new FileSystemDeploymentService(relativeTo, new File(pathName), relativePath, controllerValue.getValue().createClient(scheduledExecutorValue.getValue()), scheduledExecutorValue.getValue());
            scanner.setScanInterval(unit.toMillis(interval));
            scanner.setAutoDeployExplodedContent(autoDeployExploded);
            scanner.setAutoDeployZippedContent(autoDeployZipped);
            scanner.setAutoDeployXMLContent(autoDeployXml);
            if (deploymentTimeout != null) {
                scanner.setDeploymentTimeout(deploymentTimeout);
            }

            if (enabled) {
                scanner.startScanner();
            }
            this.scanner = scanner;
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop(StopContext context) {
        final DeploymentScanner scanner = this.scanner;
        this.scanner = null;
        scanner.stopScanner();
        scheduledExecutorValue.getValue().shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized DeploymentScanner getValue() throws IllegalStateException {
        final DeploymentScanner scanner = this.scanner;
        if (scanner == null) {
            throw new IllegalStateException();
        }
        return scanner;
    }

}
