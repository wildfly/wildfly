/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.composite;

import static org.jboss.as.ejb3.logging.EjbLogger.EJB3_TIMER_LOGGER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.TimerConfig;

import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceRegistry;

/**
 * A composite timer service that manages persistent vs transient timers separately.
 * @author Paul Ferraro
 */
public class CompositeTimerService implements ManagedTimerService {

    private final TimedObjectInvoker invoker;
    private final TimerServiceRegistry registry;
    private final ManagedTimerService transientTimerService;
    private final ManagedTimerService persistentTimerService;

    public CompositeTimerService(CompositeTimerServiceConfiguration configuration) {
        this.invoker = configuration.getInvoker();
        this.registry = configuration.getTimerServiceRegistry();
        this.transientTimerService = configuration.getTransientTimerService();
        this.persistentTimerService = configuration.getPersistentTimerService();
    }

    @Override
    public TimedObjectInvoker getInvoker() {
        return this.invoker;
    }

    @Override
    public boolean isStarted() {
        return this.transientTimerService.isStarted() && this.persistentTimerService.isStarted();
    }

    @Override
    public void start() {
        this.transientTimerService.start();
        this.persistentTimerService.start();
    }

    @Override
    public void stop() {
        this.persistentTimerService.stop();
        this.transientTimerService.stop();
    }

    @Override
    public void close() {
        this.persistentTimerService.close();
        this.transientTimerService.close();
    }

    private ManagedTimerService getTimerService(TimerConfig config) {
        return config.isPersistent() ? this.persistentTimerService : this.transientTimerService;
    }

    @Override
    public jakarta.ejb.Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig config) {
        this.validateInvocationContext();
        if (schedule == null) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("schedule", null);
        }
        TimerConfig timerConfig = (config != null) ? config : new TimerConfig();
        return this.getTimerService(timerConfig).createCalendarTimer(schedule, timerConfig);
    }

    @Override
    public jakarta.ejb.Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig config) {
        this.validateInvocationContext();
        if (initialExpiration == null) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("initialExpiration", null);
        }
        if (initialExpiration.getTime() < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("initialExpiration.getTime()", Long.toString(initialExpiration.getTime()));
        }
        if (intervalDuration < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("intervalDuration", Long.toString(intervalDuration));
        }
        TimerConfig timerConfig = (config != null) ? config : new TimerConfig();
        return this.getTimerService(timerConfig).createIntervalTimer(initialExpiration, intervalDuration, timerConfig);
    }

    @Override
    public jakarta.ejb.Timer createSingleActionTimer(Date expiration, TimerConfig config) {
        this.validateInvocationContext();
        if (expiration == null) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("expiration", null);
        }
        if (expiration.getTime() < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("expiration.getTime()", Long.toString(expiration.getTime()));
        }
        TimerConfig timerConfig = (config != null) ? config : new TimerConfig();
        return this.getTimerService(timerConfig).createSingleActionTimer(expiration, timerConfig);
    }

    @Override
    public Collection<jakarta.ejb.Timer> getTimers() {
        Collection<jakarta.ejb.Timer> transientTimers = this.transientTimerService.getTimers();
        Collection<jakarta.ejb.Timer> persistentTimers = this.persistentTimerService.getTimers();
        Collection<jakarta.ejb.Timer> result = new ArrayList<>(transientTimers.size() + persistentTimers.size());
        result.addAll(transientTimers);
        result.addAll(persistentTimers);
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public Collection<jakarta.ejb.Timer> getAllTimers() {
        return this.registry.getAllTimers();
    }

    @Override
    public ManagedTimer findTimer(String id) {
        ManagedTimer timer = this.transientTimerService.findTimer(id);
        if (timer == null) {
            timer = this.persistentTimerService.findTimer(id);
        }
        return timer;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.getClass().getSimpleName(), this.invoker.getTimedObjectId());
    }
}
