/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import java.util.Collection;
import java.util.Date;

import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;

import org.jboss.as.clustering.service.DecoratedBlockingLifecycle;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.wildfly.service.BlockingLifecycle;

/**
 * Decorator of a managed TimerService.
 * @author Paul Ferraro
 */
public class DecoratedTimerService extends DecoratedBlockingLifecycle implements ManagedTimerService {

    private final ManagedTimerService service;

    public DecoratedTimerService(ManagedTimerService service) {
        this(service, service);
    }

    public DecoratedTimerService(ManagedTimerService service, BlockingLifecycle lifecycle) {
        super(lifecycle);
        this.service = service;
    }

    @Override
    public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig) {
        return this.service.createSingleActionTimer(expiration, timerConfig);
    }

    @Override
    public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig timerConfig) {
        return this.service.createIntervalTimer(initialExpiration, intervalDuration, timerConfig);
    }

    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig) {
        return this.service.createCalendarTimer(schedule, timerConfig);
    }

    @Override
    public Collection<Timer> getTimers() {
        return this.service.getTimers();
    }

    @Override
    public Collection<Timer> getAllTimers() {
        return this.service.getAllTimers();
    }

    @Override
    public void close() {
        this.service.close();
    }

    @Override
    public ManagedTimer findTimer(String id) {
        return this.service.findTimer(id);
    }

    @Override
    public TimedObjectInvoker getInvoker() {
        return this.service.getInvoker();
    }
}
