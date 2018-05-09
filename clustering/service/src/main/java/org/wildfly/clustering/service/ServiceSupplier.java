/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StabilityMonitor;

/**
 * Returns the value supplied by a {@link Service}.
 * @author Paul Ferraro
 */
public class ServiceSupplier<T> implements Supplier<T> {

    private final ServiceRegistry registry;
    private final ServiceName name;
    private final ServiceController.Mode mode;

    private volatile Duration duration = null;

    ServiceSupplier(ServiceRegistry registry, ServiceName name, ServiceController.Mode mode) {
        this.registry = registry;
        this.name = name;
        this.mode = mode;
    }

    public ServiceSupplier<T> setTimeout(Duration duration) {
        this.duration = duration;
        return this;
    }

    @Override
    public T get() {
        ServiceTarget target = this.registry.getRequiredService(this.name).getServiceContainer();
        // Create one-time service name
        ServiceName name = this.name.append(UUID.randomUUID().toString());

        ServiceBuilder<?> builder = target.addService(name);
        Supplier<T> supplier = builder.requires(this.name);
        ServiceController<?> controller = builder.setInitialMode(this.mode).install();

        StabilityMonitor monitor = new StabilityMonitor();
        monitor.addController(controller);
        try {
            Duration duration = this.duration;
            if (duration == null) {
                monitor.awaitStability();
            } else if (!monitor.awaitStability(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(new TimeoutException());
            }
            switch (controller.getState()) {
                case START_FAILED: {
                    throw new IllegalStateException(controller.getStartException());
                }
                case UP: {
                    return supplier.get();
                }
                default: {
                    // Otherwise target service is not started
                    return null;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } finally {
            monitor.removeController(controller);
            controller.setMode(ServiceController.Mode.REMOVE);
        }
    }
}
