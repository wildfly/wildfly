/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import java.util.function.Predicate;

import jakarta.ejb.TimerConfig;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceConfiguration;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactory;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceFactoryConfiguration;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvokerFactory;
import org.jboss.as.ejb3.timerservice.spi.TimerListener;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceRegistry;

/**
 * Configures a service that provides a non-function TimerService factory.
 * @author Paul Ferraro
 */
public class NonFunctionalTimerServiceFactory implements ManagedTimerServiceFactory {

    private final String message;
    private final TimerServiceRegistry registry;
    private final TimedObjectInvokerFactory invokerFactory;
    private final TimerListener listener;

    public NonFunctionalTimerServiceFactory(String message, ManagedTimerServiceFactoryConfiguration configuration) {
        this.message = message;
        this.invokerFactory = configuration.getInvokerFactory();
        this.registry = configuration.getTimerServiceRegistry();
        this.listener = configuration.getTimerListener();
    }

    @Override
    public ManagedTimerService createTimerService(EJBComponent component) {
        TimedObjectInvoker invoker = this.invokerFactory.createInvoker(component);
        TimerServiceRegistry registry = this.registry;
        TimerListener listener = this.listener;
        return new NonFunctionalTimerService(this.message, new ManagedTimerServiceConfiguration() {
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
            public Predicate<TimerConfig> getTimerFilter() {
                return TimerFilter.ALL;
            }
        });
    }
}
