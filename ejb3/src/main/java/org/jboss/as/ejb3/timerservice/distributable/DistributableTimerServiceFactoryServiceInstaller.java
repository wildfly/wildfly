/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.distributable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.ejb.TimerConfig;

import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.management.Capabilities;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.timerservice.SuspendableTimerService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactory;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactoryConfiguration;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvokerFactory;
import org.jboss.as.ejb3.timerservice.spi.TimerListener;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceRegistry;
import org.jboss.msc.service.ServiceController;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.timer.TimeoutListener;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.TimerManagerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagerFactory;
import org.wildfly.clustering.ejb.timer.TimerManagerFactoryConfiguration;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.ejb.timer.TimerServiceConfiguration;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.util.UUIDFactory;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a service that provides a distributed {@link TimerServiceFactory}.
 * @author Paul Ferraro
 */
public class DistributableTimerServiceFactoryServiceInstaller implements ServiceInstaller {

    enum TimerIdentifierFactory implements Supplier<java.util.UUID>, Function<String, UUID> {
        INSTANCE;

        @Override
        public java.util.UUID get() {
            return UUIDFactory.INSECURE.get();
        }

        @Override
        public java.util.UUID apply(String id) {
            return java.util.UUID.fromString(id);
        }
    }

    private final ServiceName name;
    private final ManagedTimerServiceFactoryConfiguration factoryConfiguration;
    private final TimerServiceConfiguration configuration;
    private final java.util.function.Supplier<TimerManagementProvider> provider;
    private final Predicate<TimerConfig> filter;

    public DistributableTimerServiceFactoryServiceInstaller(ServiceName name, ManagedTimerServiceFactoryConfiguration factoryConfiguration, TimerServiceConfiguration configuration, java.util.function.Supplier<TimerManagementProvider> provider, Predicate<TimerConfig> filter) {
        this.name = name;
        this.factoryConfiguration = factoryConfiguration;
        this.configuration = configuration;
        this.provider = provider;
        this.filter = filter;
    }

    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        TimerServiceRegistry registry = this.factoryConfiguration.getTimerServiceRegistry();
        TimedObjectInvokerFactory invokerFactory = this.factoryConfiguration.getInvokerFactory();
        TimerListener listener = this.factoryConfiguration.getTimerListener();
        Predicate<TimerConfig> filter = this.filter;
        TimerServiceConfiguration configuration = this.configuration;
        boolean persistent = filter.test(new TimerConfig(null, true));
        Supplier<UUID> identifierFactory = TimerIdentifierFactory.INSTANCE;

        TimerRegistry<UUID> timerRegistry = new TimerRegistry<>() {
            @Override
            public void register(UUID id) {
                listener.timerAdded(id.toString());
            }

            @Override
            public void unregister(UUID id) {
                listener.timerRemoved(id.toString());
            }
        };
        TimerManagerFactoryConfiguration<UUID> managerFactoryConfiguration = new TimerManagerFactoryConfiguration<>() {
            @Override
            public boolean isPersistent() {
                return persistent;
            }

            @Override
            public TimerServiceConfiguration getTimerServiceConfiguration() {
                return configuration;
            }

            @Override
            public TimerRegistry<UUID> getRegistry() {
                return timerRegistry;
            }

            @Override
            public Supplier<UUID> getIdentifierFactory() {
                return identifierFactory;
            }
        };

        ServiceName timerManagerFactoryName = ServiceName.JBOSS.append("clustering", "timer", this.configuration.getName());
        for (ServiceInstaller installer : this.provider.get().getTimerManagerFactoryServiceInstallers(timerManagerFactoryName, managerFactoryConfiguration)) {
            installer.install(target);
        }

        ServiceDependency<TimerManagerFactory<UUID>> managerFactory = ServiceDependency.on(timerManagerFactoryName);
        ServiceDependency<SuspendableActivityRegistry> activityRegistry = ServiceDependency.on(SuspendableActivityRegistry.SERVICE_DESCRIPTOR);
        ServiceDependency<Executor> executor = ServiceDependency.on(Capabilities.MANAGEMENT_EXECUTOR);

        ManagedTimerServiceFactory factory = new ManagedTimerServiceFactory() {
            @Override
            public ManagedTimerService createTimerService(EJBComponent component) {
                TimedObjectInvoker invoker = invokerFactory.createInvoker(component);
                Consumer<Timer<UUID>> activateTask = new Consumer<>() {
                    @Override
                    public void accept(Timer<UUID> timer) {
                        timer.activate();
                        timerRegistry.register(timer.getId());
                    }
                };
                Consumer<Timer<UUID>> cancelTask = new Consumer<>() {
                    @Override
                    public void accept(Timer<UUID> timer) {
                        timerRegistry.unregister(timer.getId());
                        timer.cancel();
                    }
                };
                TimerSynchronizationFactory<UUID> synchronizationFactory = new DistributableTimerSynchronizationFactory<>(activateTask, cancelTask);
                TimeoutListener<UUID> timeoutListener = new DistributableTimeoutListener<>(invoker, synchronizationFactory);
                TimerManager<UUID> manager = managerFactory.get().createTimerManager(new TimerManagerConfiguration<>() {
                    @Override
                    public TimerServiceConfiguration getTimerServiceConfiguration() {
                        return configuration;
                    }

                    @Override
                    public Supplier<UUID> getIdentifierFactory() {
                        return identifierFactory;
                    }

                    @Override
                    public TimerRegistry<UUID> getRegistry() {
                        return timerRegistry;
                    }

                    @Override
                    public boolean isPersistent() {
                        return persistent;
                    }

                    @Override
                    public TimeoutListener<UUID> getListener() {
                        return timeoutListener;
                    }
                });
                DistributableTimerServiceConfiguration<UUID> serviceConfiguration = new DistributableTimerServiceConfiguration<>() {
                    @Override
                    public TimedObjectInvoker getInvoker() {
                        return invoker;
                    }

                    @Override
                    public TimerServiceRegistry getTimerServiceRegistry() {
                        return registry;
                    }

                    @Override
                    public TimerListener getTimerListener() {
                        return listener;
                    }

                    @Override
                    public Function<String, UUID> getIdentifierParser() {
                        return TimerIdentifierFactory.INSTANCE;
                    }

                    @Override
                    public Predicate<TimerConfig> getTimerFilter() {
                        return filter;
                    }

                    @Override
                    public TimerSynchronizationFactory<UUID> getTimerSynchronizationFactory() {
                        return synchronizationFactory;
                    }
                };
                return new SuspendableTimerService(new DistributableTimerService<>(serviceConfiguration, manager), activityRegistry.get(), executor.get());
            }
        };
        return ServiceInstaller.builder(factory)
                .provides(this.name)
                .startWhen(StartWhen.REQUIRED)
                .requires(List.of(managerFactory, activityRegistry, executor))
                .build()
                .install(target);
    }
}
