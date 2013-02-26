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

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.component.singleton.SingletonComponent;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.invocation.InterceptorContext;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * Non-functional timer service that is bound when the timer service is disabled.
 */
public class NonFunctionalTimerService implements TimerService {

    public static final NonFunctionalTimerService DISABLED = new NonFunctionalTimerService(EjbMessages.MESSAGES.timerServiceIsNotActive());
    public static final NonFunctionalTimerService NO_TIMER_METHODS = new NonFunctionalTimerService(EjbMessages.MESSAGES.ejbHasNoTimerMethods());

    private final String message;

    private NonFunctionalTimerService(final String message) {
        this.message = message;
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
                throw MESSAGES.failToInvokeTimerServiceDoLifecycle();
            }
        }
    }
}
