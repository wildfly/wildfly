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

package org.jboss.as.ejb3.timerservice.distributable;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.ejb.TimerConfig;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
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
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.UUIDFactory;
import org.wildfly.clustering.ejb.BeanConfiguration;
import org.wildfly.clustering.ejb.timer.TimeoutListener;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.TimerManagerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagerFactory;
import org.wildfly.clustering.ejb.timer.TimerManagerFactoryConfiguration;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class DistributableTimerServiceFactoryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, TimerManagerFactoryConfiguration<UUID>, ManagedTimerServiceFactory, TimerRegistry<UUID> {

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

    private volatile SupplierDependency<TimerManagerFactory<UUID, Batch>> factory;

    private final TimerServiceRegistry registry;
    private final TimedObjectInvokerFactory invokerFactory;
    private final BeanConfiguration config;
    private final TimerListener registrar;
    private final TimerManagementProvider provider;
    private final Predicate<TimerConfig> filter;

    private volatile ServiceConfigurator configurator;

    public DistributableTimerServiceFactoryServiceConfigurator(ServiceName name, ManagedTimerServiceFactoryConfiguration factoryConfiguration, BeanConfiguration beanConfiguration, TimerManagementProvider provider, Predicate<TimerConfig> filter) {
        super(name);
        this.registry = factoryConfiguration.getTimerServiceRegistry();
        this.invokerFactory = factoryConfiguration.getInvokerFactory();
        this.config = beanConfiguration;
        this.registrar = factoryConfiguration.getTimerListener();
        this.provider = provider;
        this.filter = filter;
    }

    @Override
    public ManagedTimerService createTimerService(EJBComponent component) {
        TimedObjectInvoker invoker = this.invokerFactory.createInvoker(component);
        TimerServiceRegistry registry = this.registry;
        TimerListener timerListener = this.registrar;
        Predicate<TimerConfig> filter = this.filter;
        BeanConfiguration beanConfig = this.config;
        TimerSynchronizationFactory<UUID> synchronizationFactory = new DistributableTimerSynchronizationFactory<>(this.getRegistry());
        TimeoutListener<UUID, Batch> timeoutListener = new DistributableTimeoutListener<>(invoker, synchronizationFactory);
        TimerManager<UUID, Batch> manager = this.factory.get().createTimerManager(new TimerManagerConfiguration<UUID, Batch>() {
            @Override
            public BeanConfiguration getBeanConfiguration() {
                return beanConfig;
            }

            @Override
            public Supplier<UUID> getIdentifierFactory() {
                return DistributableTimerServiceFactoryServiceConfigurator.this.getIdentifierFactory();
            }

            @Override
            public TimerRegistry<UUID> getRegistry() {
                return DistributableTimerServiceFactoryServiceConfigurator.this.getRegistry();
            }

            @Override
            public boolean isPersistent() {
                return DistributableTimerServiceFactoryServiceConfigurator.this.isPersistent();
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
                return timerListener;
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
        return new DistributableTimerService<>(serviceConfiguration, manager);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.configurator = this.provider.getTimerManagerFactoryServiceConfigurator(this).configure(support);

        this.factory = new ServiceSupplierDependency<>(this.configurator);

        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.configurator.build(target).install();

        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<ManagedTimerServiceFactory> factory = this.factory.register(builder).provides(name);
        return builder.setInstance(Service.newInstance(factory, this)).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Supplier<UUID> getIdentifierFactory() {
        return TimerIdentifierFactory.INSTANCE;
    }

    @Override
    public TimerRegistry<UUID> getRegistry() {
        return this;
    }

    @Override
    public void register(UUID id) {
        this.registrar.timerAdded(id.toString());
    }

    @Override
    public void unregister(UUID id) {
        this.registrar.timerRemoved(id.toString());
    }

    @Override
    public boolean isPersistent() {
        return this.filter.test(new TimerConfig(null, true));
    }

    @Override
    public BeanConfiguration getBeanConfiguration() {
        return this.config;
    }
}
