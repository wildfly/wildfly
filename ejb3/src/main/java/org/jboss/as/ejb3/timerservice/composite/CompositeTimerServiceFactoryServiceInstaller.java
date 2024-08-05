/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.composite;

import java.util.List;
import java.util.function.Predicate;

import jakarta.ejb.TimerConfig;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceConfiguration.TimerFilter;
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
 * @author Paul Ferraro
 */
public class CompositeTimerServiceFactoryServiceInstaller implements DeploymentServiceInstaller {

    private final ServiceName name;
    private final ManagedTimerServiceFactoryConfiguration configuration;

    public CompositeTimerServiceFactoryServiceInstaller(ServiceName name, ManagedTimerServiceFactoryConfiguration configuration) {
        this.name = name;
        this.configuration = configuration;
    }

    @Override
    public void install(DeploymentPhaseContext context) {
        TimedObjectInvokerFactory invokerFactory = this.configuration.getInvokerFactory();
        TimerServiceRegistry registry = this.configuration.getTimerServiceRegistry();
        ServiceDependency<ManagedTimerServiceFactory> transientFactory = ServiceDependency.on(TimerFilter.TRANSIENT.apply(this.name));
        ServiceDependency<ManagedTimerServiceFactory> persistentFactory = ServiceDependency.on(TimerFilter.PERSISTENT.apply(this.name));
        ManagedTimerServiceFactory factory = new ManagedTimerServiceFactory() {
            @Override
            public ManagedTimerService createTimerService(EJBComponent component) {
                TimedObjectInvoker invoker = invokerFactory.createInvoker(component);
                ManagedTimerService transientTimerService = transientFactory.get().createTimerService(component);
                ManagedTimerService persistentTimerService = persistentFactory.get().createTimerService(component);
                return new CompositeTimerService(new CompositeTimerServiceConfiguration() {
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
                        return null;
                    }

                    @Override
                    public ManagedTimerService getTransientTimerService() {
                        return transientTimerService;
                    }

                    @Override
                    public ManagedTimerService getPersistentTimerService() {
                        return persistentTimerService;
                    }

                    @Override
                    public Predicate<TimerConfig> getTimerFilter() {
                        return TimerFilter.ALL;
                    }
                });
            }
        };
        ServiceInstaller.builder(Functions.constantSupplier(factory))
                .provides(this.name)
                .requires(List.of(transientFactory, persistentFactory))
                .build()
                .install(context);
    }
}
