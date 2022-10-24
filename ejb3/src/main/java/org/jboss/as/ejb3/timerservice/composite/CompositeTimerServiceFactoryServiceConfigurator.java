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
