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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import jakarta.ejb.EJBException;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;

import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceConfiguration;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceRegistry;

/**
 * Non-functional timer service that is bound when the timer service is disabled.
 */
public class NonFunctionalTimerService implements ManagedTimerService {

    private final String message;
    private final TimerServiceRegistry timerServiceRegistry;
    private final TimedObjectInvoker invoker;

    public NonFunctionalTimerService(final String message, ManagedTimerServiceConfiguration configuration) {
        this.message = message;
        this.timerServiceRegistry = configuration.getTimerServiceRegistry();
        this.invoker = configuration.getInvoker();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public ManagedTimer findTimer(String id) {
        return null;
    }

    @Override
    public TimedObjectInvoker getInvoker() {
        return this.invoker;
    }

    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig) {
        throw new IllegalStateException(this.message);
    }

    @Override
    public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig timerConfig) {
        throw new IllegalStateException(this.message);
    }

    @Override
    public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig) {
        throw new IllegalStateException(this.message);
    }

    @Override
    public Collection<Timer> getTimers() {
        this.validateInvocationContext();

        return Collections.emptySet();
    }

    @Override
    public Collection<jakarta.ejb.Timer> getAllTimers() throws IllegalStateException, EJBException {
        this.validateInvocationContext();

        return this.timerServiceRegistry.getAllTimers();
    }
}
