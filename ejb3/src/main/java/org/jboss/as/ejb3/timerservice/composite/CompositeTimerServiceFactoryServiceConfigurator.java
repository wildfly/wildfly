/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.composite;

import java.util.function.Consumer;
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
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a services that provides a composite timer service factory.
 * @author Paul Ferraro
 */
public class CompositeTimerServiceFactoryServiceConfigurator extends SimpleServiceNameProvider implements ServiceConfigurator, ManagedTimerServiceFactory {

    private final TimedObjectInvokerFactory invokerFactory;
    private final TimerServiceRegistry registry;
    private final SupplierDependency<ManagedTimerServiceFactory> transientFactory;
    private final SupplierDependency<ManagedTimerServiceFactory> persistentFactory;

    public CompositeTimerServiceFactoryServiceConfigurator(ServiceName name, ManagedTimerServiceFactoryConfiguration configuration) {
        super(name);
        this.invokerFactory = configuration.getInvokerFactory();
        this.registry = configuration.getTimerServiceRegistry();
        this.transientFactory = new ServiceSupplierDependency<>(TimerFilter.TRANSIENT.apply(name));
        this.persistentFactory = new ServiceSupplierDependency<>(TimerFilter.PERSISTENT.apply(name));
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<ManagedTimerServiceFactory> factory = new CompositeDependency(this.transientFactory, this.persistentFactory).register(builder).provides(name);
        return builder.setInstance(Service.newInstance(factory, this)).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ManagedTimerService createTimerService(EJBComponent component) {
        TimedObjectInvoker invoker = this.invokerFactory.createInvoker(component);
        ManagedTimerService transientTimerService = this.transientFactory.get().createTimerService(component);
        ManagedTimerService persistentTimerService = this.persistentFactory.get().createTimerService(component);
        TimerServiceRegistry registry = this.registry;
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
}
