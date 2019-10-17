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
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Returns the value supplied by a {@link Service}.
 * @author Paul Ferraro
 */
@Deprecated
public class ServiceSupplier<T> implements Supplier<T> {

    private final Supplier<ServiceController<?>> factory;
    private final ServiceController.Mode mode;

    private volatile Duration duration = null;

    ServiceSupplier(ServiceRegistry registry, ServiceName name, ServiceController.Mode mode) {
        this.factory = new PrivilegedActionSupplier<ServiceController<?>>() {
            @Override
            public ServiceController<?> run() {
                return registry.getRequiredService(name);
            }
        };
        this.mode = mode;
    }

    public ServiceSupplier<T> setTimeout(Duration duration) {
        this.duration = duration;
        return this;
    }

    @Override
    public T get() {
        ServiceController<?> sourceController = this.factory.get();
        ServiceName sourceName = sourceController.getName();
        ServiceTarget target = sourceController.getServiceContainer();
        // Create one-time service name
        ServiceName name = sourceName.append(UUID.randomUUID().toString());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch removeLatch = new CountDownLatch(1);
        ServiceBuilder<?> builder = target.addService(name);
        Supplier<T> supplier = builder.requires(sourceName);
        Reference<T> reference = new Reference<>();
        ServiceController<?> controller = builder.setInstance(new FunctionalService<>(reference, Function.identity(), supplier))
                .addListener(new CountDownLifecycleListener(startLatch, EnumSet.of(LifecycleEvent.UP, LifecycleEvent.FAILED)))
                .addListener(new CountDownLifecycleListener(removeLatch, EnumSet.of(LifecycleEvent.REMOVED)))
                .setInitialMode(this.mode)
                .install();

        try {
            // Don't wait for start latch if there are unavailable dependencies
            if (controller.getUnavailableDependencies().isEmpty()) {
                Duration duration = this.duration;
                if (duration == null) {
                    startLatch.await();
                } else if (!startLatch.await(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException(new TimeoutException());
                }
            }

            return reference.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } finally {
            controller.setMode(ServiceController.Mode.REMOVE);
            try {
                removeLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class Reference<T> implements Supplier<T>, Consumer<T> {
        private volatile T value = null;

        @Override
        public void accept(T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return this.value;
        }
    }
}
