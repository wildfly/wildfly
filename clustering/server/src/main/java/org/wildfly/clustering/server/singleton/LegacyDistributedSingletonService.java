/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.singleton;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.singleton.SingletonService;

/**
 * Distributed {@link SingletonService} implementation that uses JBoss MSC 1.3.x service installation.
 * Decorates an MSC service ensuring that it is only started on one node in the cluster at any given time.
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyDistributedSingletonService<T> extends AbstractDistributedSingletonService<LegacySingletonContext<T>> implements SingletonService<T>, LegacySingletonContext<T>, PrimaryProxyContext<T> {

    private final ServiceName name;

    private volatile boolean started = false;
    private volatile ServiceController<T> primaryController;
    private volatile ServiceController<T> backupController;

    public LegacyDistributedSingletonService(DistributedSingletonServiceContext context, Service<T> primaryService, Service<T> backupService) {
        this(context, primaryService, backupService, new LazySupplier<>());
    }

    private LegacyDistributedSingletonService(DistributedSingletonServiceContext context, Service<T> primaryService, Service<T> backupService, LazySupplier<PrimaryProxyContext<T>> contextFactory) {
        super(context, new ServiceLifecycleFactory<>(context.getServiceName(), primaryService, (backupService != null) ? backupService : new PrimaryProxyService<>(contextFactory)));
        contextFactory.accept(this);
        this.name = context.getServiceName();
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public LegacySingletonContext<T> get() {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        ServiceRegistry registry = context.getController().getServiceContainer();
        this.primaryController = (ServiceController<T>) registry.getService(this.getServiceName().append("primary"));
        this.backupController = (ServiceController<T>) registry.getService(this.getServiceName().append("backup"));
        this.started = true;
    }

    @Override
    public void stop(StopContext context) {
        this.started = false;
        super.stop(context);
    }

    @Override
    public T getValue() {
        while (this.started) {
            try {
                return (this.isPrimary() ? this.primaryController : this.backupController).getValue();
            } catch (IllegalStateException e) {
                // Verify whether ISE is due to unmet quorum in the previous election
                if (this.getServiceProviderRegistration().getProviders().size() < this.getQuorum()) {
                    throw ClusteringServerLogger.ROOT_LOGGER.notStarted(this.getServiceName().getCanonicalName());
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw e;
                }
                // Otherwise, we're in the midst of a new election, so just try again
                Thread.yield();
            }
        }
        throw ClusteringServerLogger.ROOT_LOGGER.notStarted(this.getServiceName().getCanonicalName());
    }

    @Override
    public CommandDispatcher<LegacySingletonContext<T>> getCommandDispatcher() {
        return super.getCommandDispatcher();
    }

    @Override
    public Optional<T> getLocalValue() {
        try {
            return this.isPrimary() ? Optional.ofNullable(this.primaryController.getValue()) : null;
        } catch (IllegalStateException e) {
            // This might happen if primary service has not yet started, or if node is no longer the primary node
            return null;
        }
    }

    static class LazySupplier<T> implements Supplier<T>, Consumer<T> {
        private volatile T value;

        @Override
        public void accept(T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return this.value;
        }
    }

    private static class ServiceLifecycleFactory<T> implements Function<ServiceTarget, Lifecycle> {
        private final ServiceName name;
        private final Service<T> primaryService;
        private final Service<T> backupService;

        ServiceLifecycleFactory(ServiceName name, Service<T> primaryService, Service<T> backupService) {
            this.name = name;
            this.primaryService = primaryService;
            this.backupService = backupService;
        }

        @Override
        public Lifecycle apply(ServiceTarget target) {
            Lifecycle primaryLifecycle = new ServiceLifecycle(target.addService(this.name.append("primary"), this.primaryService).setInitialMode(ServiceController.Mode.NEVER).install());
            Lifecycle backupLifecycle = new ServiceLifecycle(target.addService(this.name.append("backup"), this.backupService).setInitialMode(ServiceController.Mode.ACTIVE).install());
            return new PrimaryBackupLifecycle(primaryLifecycle, backupLifecycle);
        }
    }

    private static class PrimaryBackupLifecycle implements Lifecycle {
        private final Lifecycle primaryLifecycle;
        private final Lifecycle backupLifecycle;

        PrimaryBackupLifecycle(Lifecycle primaryLifecycle, Lifecycle backupLifecycle) {
            this.primaryLifecycle = primaryLifecycle;
            this.backupLifecycle = backupLifecycle;
        }

        @Override
        public void start() {
            this.backupLifecycle.stop();
            this.primaryLifecycle.start();
        }

        @Override
        public void stop() {
            this.primaryLifecycle.stop();
            this.backupLifecycle.start();
        }
    }
}
