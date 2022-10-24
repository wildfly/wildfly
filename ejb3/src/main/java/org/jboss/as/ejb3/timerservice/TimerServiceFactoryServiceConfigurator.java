/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.timerservice;

import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.ejb.TimerConfig;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.subsystem.TimerServiceResourceDefinition;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
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
 * Configures a service that provides a local timer service factory.
 * @author Paul Ferraro
 */
public class TimerServiceFactoryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, ManagedTimerServiceFactory {

    private final TimerServiceRegistry registry;
    private final TimerListener listener;
    private final String threadPoolName;
    private final String store;
    private final TimedObjectInvokerFactory invokerFactory;

    private volatile SupplierDependency<Timer> timer;
    private volatile SupplierDependency<ExecutorService> executor;
    private volatile SupplierDependency<TimerPersistence> persistence;
    private volatile Predicate<TimerConfig> timerFilter = TimerFilter.ALL;

    public TimerServiceFactoryServiceConfigurator(ServiceName name, ManagedTimerServiceFactoryConfiguration configuration, String threadPoolName, String store) {
        super(name);
        this.invokerFactory = configuration.getInvokerFactory();
        this.registry = configuration.getTimerServiceRegistry();
        this.listener = configuration.getTimerListener();
        this.threadPoolName = threadPoolName;
        this.store = store;
    }

    public TimerServiceFactoryServiceConfigurator filter(Predicate<TimerConfig> timerFilter) {
        this.timerFilter = timerFilter;
        return this;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.timer = new ServiceSupplierDependency<>(support.getCapabilityServiceName(TimerServiceResourceDefinition.TIMER_SERVICE_CAPABILITY_NAME));
        this.executor = new ServiceSupplierDependency<>(support.getCapabilityServiceName(TimerServiceResourceDefinition.THREAD_POOL_CAPABILITY_NAME, this.threadPoolName));
        this.persistence = (this.store != null) ? new ServiceSupplierDependency<>(support.getCapabilityServiceName(TimerServiceResourceDefinition.TIMER_PERSISTENCE_CAPABILITY_NAME, this.store)) : null;
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<ManagedTimerServiceFactory> factory = new CompositeDependency(this.timer, this.executor, this.persistence).register(builder).provides(name);
        return builder.setInstance(Service.newInstance(factory, this)).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ManagedTimerService createTimerService(EJBComponent component) {
        TimedObjectInvoker invoker = this.invokerFactory.createInvoker(component);
        TimerServiceRegistry registry = this.registry;
        TimerListener listener = this.listener;
        ExecutorService executor = this.executor.get();
        Timer timer = this.timer.get();
        TimerPersistence persistence = (this.persistence != null) ? this.persistence.get() : null;
        Predicate<TimerConfig> timerFilter = this.timerFilter;
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
            public ExecutorService getExecutor() {
                return executor;
            }

            @Override
            public Timer getTimer() {
                return timer;
            }

            @Override
            public TimerPersistence getTimerPersistence() {
                return persistence;
            }

            @Override
            public Predicate<TimerConfig> getTimerFilter() {
                return timerFilter;
            }
        });
    }
}
