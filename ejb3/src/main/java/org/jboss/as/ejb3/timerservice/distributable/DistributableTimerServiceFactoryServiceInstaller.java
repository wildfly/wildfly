/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.distributable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.ejb.TimerConfig;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.timerservice.SuspendableTimerService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactory;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactoryConfiguration;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvokerFactory;
import org.jboss.as.ejb3.timerservice.spi.TimerListener;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceRegistry;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.as.server.suspend.SuspensionStateProvider;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.UUIDFactory;
import org.wildfly.clustering.ejb.timer.TimeoutListener;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.TimerManagerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagerFactory;
import org.wildfly.clustering.ejb.timer.TimerManagerFactoryConfiguration;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.ejb.timer.TimerServiceConfiguration;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a service that provides a distributed {@link TimerServiceFactory}.
 * @author Paul Ferraro
 */
public class DistributableTimerServiceFactoryServiceInstaller implements DeploymentServiceInstaller {

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
    private final TimerManagementProvider provider;
    private final Predicate<TimerConfig> filter;

    public DistributableTimerServiceFactoryServiceInstaller(ServiceName name, ManagedTimerServiceFactoryConfiguration factoryConfiguration, TimerServiceConfiguration configuration, TimerManagementProvider provider, Predicate<TimerConfig> filter) {
        this.name = name;
        this.factoryConfiguration = factoryConfiguration;
        this.configuration = configuration;
        this.provider = provider;
        this.filter = filter;
    }

    @Override
    public void install(DeploymentPhaseContext context) {
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
        ServiceConfigurator configurator = this.provider.getTimerManagerFactoryServiceConfigurator(managerFactoryConfiguration).configure(context.getDeploymentUnit().getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT));
        configurator.build(context.getRequirementServiceTarget()).install();

        ServiceDependency<TimerManagerFactory<UUID, Batch>> managerFactory = ServiceDependency.on(configurator.getServiceName());
        ServiceDependency<SuspendableActivityRegistry> activityRegistry = ServiceDependency.on(SuspendableActivityRegistry.SERVICE_DESCRIPTOR);

        ManagedTimerServiceFactory factory = new ManagedTimerServiceFactory() {
            @Override
            public ManagedTimerService createTimerService(EJBComponent component) {
                TimedObjectInvoker invoker = invokerFactory.createInvoker(component);
                SuspensionStateProvider stateProvider = activityRegistry.get();
                Consumer<Timer<UUID>> activateTask = new Consumer<>() {
                    @Override
                    public void accept(Timer<UUID> timer) {
                        // Do not activate timer if we are suspended
                        if (stateProvider.getState() == SuspensionStateProvider.State.RUNNING) {
                            timer.activate();
                        }
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
                TimeoutListener<UUID, Batch> timeoutListener = new DistributableTimeoutListener<>(invoker, synchronizationFactory);
                TimerManager<UUID, Batch> manager = managerFactory.get().createTimerManager(new TimerManagerConfiguration<UUID, Batch>() {
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
                    public TimeoutListener<UUID, Batch> getListener() {
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
                return new SuspendableTimerService(new DistributableTimerService<>(serviceConfiguration, manager), activityRegistry.get());
            }
        };
        ServiceInstaller.builder(Functions.constantSupplier(factory))
                .provides(this.name)
                .requires(List.of(managerFactory, activityRegistry))
                .build()
                .install(context);
    }
}
