/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import jakarta.ejb.TimerConfig;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition;
import org.jboss.as.ejb3.subsystem.TimerServiceResourceDefinition;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactory;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactoryConfiguration;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvokerFactory;
import org.jboss.as.ejb3.timerservice.spi.TimerListener;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceRegistry;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a service that provides a local timer service factory.
 * @author Paul Ferraro
 */
public class TimerServiceFactoryServiceInstaller implements DeploymentServiceInstaller {

    private final ServiceName name;
    private final ManagedTimerServiceFactoryConfiguration configuration;
    private final String threadPoolName;
    private final String store;
    private final Predicate<TimerConfig> filter;

    public TimerServiceFactoryServiceInstaller(ServiceName name, ManagedTimerServiceFactoryConfiguration configuration, Predicate<TimerConfig> filter, String threadPoolName, String store) {
        this.name = name;
        this.configuration = configuration;
        this.filter = filter;
        this.threadPoolName = threadPoolName;
        this.store = store;
    }

    @Override
    public void install(DeploymentPhaseContext context) {
        ServiceDependency<Executor> executor = ServiceDependency.on(EJB3SubsystemRootResourceDefinition.EXECUTOR_SERVICE_DESCRIPTOR, this.threadPoolName);
        ServiceDependency<TimerPersistence> persistence = (this.store != null) ? ServiceDependency.on(TimerPersistence.SERVICE_DESCRIPTOR, this.store) : ServiceDependency.of(null);
        ServiceDependency<Timer> timer = ServiceDependency.on(TimerServiceResourceDefinition.TIMER_SERVICE_DESCRIPTOR);
        TimedObjectInvokerFactory invokerFactory = this.configuration.getInvokerFactory();
        TimerServiceRegistry registry = this.configuration.getTimerServiceRegistry();
        TimerListener listener = this.configuration.getTimerListener();
        Predicate<TimerConfig> filter = this.filter;
        ManagedTimerServiceFactory factory = new ManagedTimerServiceFactory() {
            @Override
            public ManagedTimerService createTimerService(EJBComponent component) {
                TimedObjectInvoker invoker = invokerFactory.createInvoker(component);
                return new TimerServiceImpl(new TimerServiceConfiguration() {
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
                    public Executor getExecutor() {
                        return executor.get();
                    }

                    @Override
                    public Timer getTimer() {
                        return timer.get();
                    }

                    @Override
                    public TimerPersistence getTimerPersistence() {
                        return persistence.get();
                    }

                    @Override
                    public Predicate<TimerConfig> getTimerFilter() {
                        return filter;
                    }
                });
            }
        };
        ServiceInstaller.builder(Functions.constantSupplier(factory))
                .provides(this.name)
                .requires(List.of(executor, persistence, timer))
                .build()
                .install(context);
    }
}
