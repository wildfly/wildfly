/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
    public boolean isStarted() {
        return true;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void close() {
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
