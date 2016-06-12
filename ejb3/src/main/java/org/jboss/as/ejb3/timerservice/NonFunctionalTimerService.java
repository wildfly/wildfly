/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
 * 021101301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.timerservice;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.TimerServiceRegistry;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.component.singleton.SingletonComponent;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * Non-functional timer service that is bound when the timer service is disabled.
 */
public class NonFunctionalTimerService implements TimerService, Service<TimerService> {

    public static final NonFunctionalTimerService DISABLED = new NonFunctionalTimerService(EjbLogger.EJB3_TIMER_LOGGER.timerServiceIsNotActive(), null);

    private final String message;
    private final TimerServiceRegistry timerServiceRegistry;

    public NonFunctionalTimerService(final String message, final TimerServiceRegistry timerServiceRegistry) {
        this.message = message;
        this.timerServiceRegistry = timerServiceRegistry;
    }

    public static ServiceName serviceNameFor(final EJBComponentDescription ejbComponentDescription) {
        if (ejbComponentDescription == null || ejbComponentDescription.getServiceName() == null) {
            return null;
        }
        return ejbComponentDescription.getServiceName().append("ejb", "non-functional-timerservice");
    }

    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule) throws IllegalArgumentException, IllegalStateException, EJBException {
        throw new IllegalStateException(message);
    }

    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
        throw new IllegalStateException(message);
    }

    @Override
    public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
        throw new IllegalStateException(message);
    }

    @Override
    public Timer createIntervalTimer(long initialDuration, long intervalDuration, TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
        throw new IllegalStateException(message);
    }

    @Override
    public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
        throw new IllegalStateException(message);
    }

    @Override
    public Timer createSingleActionTimer(long duration, TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
        throw new IllegalStateException(message);
    }

    @Override
    public Timer createTimer(long duration, Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        throw new IllegalStateException(message);
    }

    @Override
    public Timer createTimer(long initialDuration, long intervalDuration, Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        throw new IllegalStateException(message);
    }

    @Override
    public Timer createTimer(Date expiration, Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        throw new IllegalStateException(message);
    }

    @Override
    public Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        throw new IllegalStateException(message);
    }

    @Override
    public Collection<Timer> getTimers() throws IllegalStateException, EJBException {
        assertInvocationAllowed();
        return Collections.emptySet();
    }

    @Override
    public Collection<Timer> getAllTimers() throws IllegalStateException, EJBException {
        // query the registry
        if (this.timerServiceRegistry != null) {
            return this.timerServiceRegistry.getAllActiveTimers();
        }
        // If we don't have the timer service registry (for whatever reason), we just invoke the getTimers() so that it can
        // do the necessary state checks before returning an empty collection (since this is a non-functional timer service)
        return this.getTimers();
    }

    private void assertInvocationAllowed() {
        AllowedMethodsInformation.checkAllowed(MethodType.TIMER_SERVICE_METHOD);
        final InterceptorContext currentInvocationContext = CurrentInvocationContext.get();
        if (currentInvocationContext == null) {
            return;
        }
        // If the method in current invocation context is null,
        // then it represents a lifecycle callback invocation
        Method invokedMethod = currentInvocationContext.getMethod();
        if (invokedMethod == null) {
            // it's a lifecycle callback
            Component component = currentInvocationContext.getPrivateData(Component.class);
            if (!(component instanceof SingletonComponent)) {
                throw EjbLogger.EJB3_TIMER_LOGGER.failToInvokeTimerServiceDoLifecycle();
            }
        }
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        if (this.timerServiceRegistry != null) {
            this.timerServiceRegistry.registerTimerService(this);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        if (this.timerServiceRegistry != null) {
            this.timerServiceRegistry.unRegisterTimerService(this);
        }
    }

    @Override
    public TimerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
