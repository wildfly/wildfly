/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.SingletonState;

/**
 * Distributed singleton service implementation that uses JBoss MSC 1.3.x service installation.
 * Decorates an MSC service ensuring that it is only started on one node in the cluster at any given time.
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyDistributedSingletonService<T> extends AbstractSingletonService<LegacySingletonContext<T>, ServiceValue<T>> implements org.wildfly.clustering.singleton.SingletonService<T>, PrimaryProxyContext<T> {

    private final SingletonServiceContext context;

    private volatile boolean started = false;
    private volatile ServiceController<T> primaryController;
    private volatile ServiceController<T> backupController;

    public LegacyDistributedSingletonService(SingletonServiceContext context, Service<T> primaryService, Service<T> backupService, Consumer<Singleton> singleton) {
        this(context, primaryService, backupService, singleton, new AtomicReference<>());
    }

    private LegacyDistributedSingletonService(SingletonServiceContext context, Service<T> primaryService, Service<T> backupService, Consumer<Singleton> singleton, AtomicReference<PrimaryProxyContext<T>> primaryProxyContext) {
        super(context, new ServiceLifecycleFactory<>(context.getServiceName(), primaryService, (backupService != null) ? backupService : new PrimaryProxyService<>(primaryProxyContext::getPlain)), LegacyDistributedSingletonContext::new, singleton);
        primaryProxyContext.setPlain(this);
        this.context = context;
    }

    @Override
    public ServiceName getServiceName() {
        return this.context.getServiceName();
    }

    @Override
    public SingletonState getSingletonState() {
        return this.get().getSingletonState();
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
                return (this.get().isPrimaryProvider() ? this.primaryController : this.backupController).getValue();
            } catch (IllegalStateException e) {
                // Verify whether ISE is due to unmet quorum in the previous election
                if (this.get().getServiceProviderRegistration().getProviders().size() < this.context.getQuorum()) {
                    throw SingletonLogger.ROOT_LOGGER.notStarted(this.getServiceName().getCanonicalName());
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw e;
                }
                // Otherwise, we're in the midst of a new election, so just try again
                Thread.yield();
            }
        }
        throw SingletonLogger.ROOT_LOGGER.notStarted(this.getServiceName().getCanonicalName());
    }

    @Override
    public CommandDispatcher<GroupMember, LegacySingletonContext<T>> getCommandDispatcher() {
        return this.get().getCommandDispatcher();
    }

    private static class ServiceLifecycleFactory<T> implements Function<ServiceTarget, ServiceValue<T>> {
        private final ServiceName name;
        private final Service<T> primaryService;
        private final Service<T> backupService;

        ServiceLifecycleFactory(ServiceName name, Service<T> primaryService, Service<T> backupService) {
            this.name = name;
            this.primaryService = primaryService;
            this.backupService = backupService;
        }

        @Override
        public ServiceValue<T> apply(ServiceTarget target) {
            ServiceValue<T> primaryLifecycle = new LegacyServiceLifecycle<>(target.addService(this.name.append("primary"), this.primaryService).setInitialMode(ServiceController.Mode.NEVER).install());
            ServiceValue<T> backupLifecycle = new LegacyServiceLifecycle<>(target.addService(this.name.append("backup"), this.backupService).setInitialMode(ServiceController.Mode.ACTIVE).install());
            return new PrimaryBackupLifecycle<>(primaryLifecycle, backupLifecycle);
        }
    }

    private static class PrimaryBackupLifecycle<T> implements ServiceValue<T> {
        private final ServiceValue<T> primaryLifecycle;
        private final ServiceValue<T> backupLifecycle;

        PrimaryBackupLifecycle(ServiceValue<T> primaryLifecycle, ServiceValue<T> backupLifecycle) {
            this.primaryLifecycle = primaryLifecycle;
            this.backupLifecycle = backupLifecycle;
        }

        @Override
        public boolean isStarted() {
            return this.primaryLifecycle.isStarted();
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

        @Override
        public T getValue() {
            return this.primaryLifecycle.getValue();
        }
    }

    private static class LegacyServiceLifecycle<T> extends ServiceControllerService implements ServiceValue<T> {
        private final ServiceController<T> controller;

        public LegacyServiceLifecycle(ServiceController<T> controller) {
            super(controller);
            this.controller = controller;
        }

        @Override
        public T getValue() {
            return this.controller.getValue();
        }
    }
}
