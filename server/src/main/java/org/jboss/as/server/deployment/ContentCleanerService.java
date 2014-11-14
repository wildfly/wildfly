/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.server.deployment;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.ModelController;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.as.server.Services;

/**
 * Service in charge with cleaning left over contents from the content repository.
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class ContentCleanerService implements Service<Void> {
    /**
     * The content repository cleaner will test content for clean-up every 5 minutes.
     */
    public static final long DEFAULT_INTERVAL = 300000L;
    /**
     * Standard ServiceName under which a service controller for an instance of
     * @code Service<ContentRepository> would be registered.
     */
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("content-repository-cleaner");

    private final InjectedValue<ModelController> controllerValue = new InjectedValue<ModelController>();
    private final InjectedValue<ScheduledExecutorService> scheduledExecutorValue = new InjectedValue<ScheduledExecutorService>();
    private final InjectedValue<ControlledProcessStateService> controlledProcessStateServiceValue = new InjectedValue<ControlledProcessStateService>();
    private final InjectedValue<ExecutorService> executorServiceValue = new InjectedValue<ExecutorService>();

    private ContentRepositoryCleaner deploymentContentCleaner;
    private final long interval;
    private final boolean server;
    private final TimeUnit unit;

    public static void addService(final ServiceTarget serviceTarget, final ServiceName scheduledExecutorServiceName) {
        final ContentCleanerService service = new ContentCleanerService(true);
        ServiceBuilder<Void> builder = serviceTarget.addService(SERVICE_NAME, service)
                .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.controllerValue)
                .addDependency(ControlledProcessStateService.SERVICE_NAME, ControlledProcessStateService.class, service.controlledProcessStateServiceValue)
                .addDependency(scheduledExecutorServiceName, ScheduledExecutorService.class, service.getScheduledExecutorValue());
        Services.addServerExecutorDependency(builder, service.executorServiceValue, false);
        builder.install();
    }

    public static void addServiceOnHostController(final ServiceTarget serviceTarget, final ServiceName hostControllerServiceName,
            final ServiceName hostControllerExecutorServiceName, final ServiceName scheduledExecutorServiceName) {
        final ContentCleanerService service = new ContentCleanerService(false);
        ServiceBuilder<Void> builder = serviceTarget.addService(SERVICE_NAME, service)
                .addDependency(hostControllerServiceName, ModelController.class, service.controllerValue)
                .addDependency(ControlledProcessStateService.SERVICE_NAME, ControlledProcessStateService.class, service.controlledProcessStateServiceValue)
                .addDependency(hostControllerExecutorServiceName, ExecutorService.class, service.executorServiceValue)
                .addDependency(scheduledExecutorServiceName, ScheduledExecutorService.class, service.getScheduledExecutorValue());
        builder.install();
    }

    ContentCleanerService(final boolean server) {
        this.interval = DEFAULT_INTERVAL;
        this.unit = TimeUnit.MILLISECONDS;
        this.server = server;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        this.deploymentContentCleaner = new ContentRepositoryCleaner(controllerValue.getValue().createClient(
                executorServiceValue.getValue()), controlledProcessStateServiceValue.getValue(),
                scheduledExecutorValue.getValue(), unit.toMillis(interval), server);
        deploymentContentCleaner.startScan();
    }

    @Override
    public synchronized void stop(StopContext context) {
        final ContentRepositoryCleaner contentCleaner = this.deploymentContentCleaner;
        this.deploymentContentCleaner = null;
        contentCleaner.stopScan();
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    public InjectedValue<ModelController> getControllerValue() {
        return controllerValue;
    }

    public InjectedValue<ScheduledExecutorService> getScheduledExecutorValue() {
        return scheduledExecutorValue;
    }
}
