/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CompletionStage;

import jakarta.ejb.EJBException;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.server.suspend.ServerResumeContext;
import org.jboss.as.server.suspend.ServerSuspendContext;
import org.jboss.as.server.suspend.ServerSuspendController;
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;

/**
 * @author Paul Ferraro
 */
public class SuspendableTimerService implements ManagedTimerService {

    private final ManagedTimerService service;
    private final SuspendableActivityRegistry registry;
    private final SuspendableActivity activity;

    public SuspendableTimerService(ManagedTimerService service, SuspendableActivityRegistry registry) {
        this.service = service;
        this.registry = registry;
        this.activity = new SuspendableActivity() {
            @Override
            public CompletionStage<Void> suspend(ServerSuspendContext context) {
                if (!context.isStarting()) { // Avoid spurious stop on startup during activity registration
                    EjbLogger.EJB3_TIMER_LOGGER.debugf("Suspending timer service for %s" , service.getInvoker());
                    service.stop();
                }
                return SuspendableActivity.COMPLETED;
            }

            @Override
            public CompletionStage<Void> resume(ServerResumeContext context) {
                EjbLogger.EJB3_TIMER_LOGGER.debugf("Resuming timer service for %s" , service.getInvoker());
                service.start();
                return SuspendableActivity.COMPLETED;
            }
        };
    }

    @Override
    public boolean isStarted() {
        return this.service.isStarted();
    }

    @Override
    public void start() {
        this.registry.registerActivity(this.activity);
        // Only start now if we are not suspended
        if (this.registry.getState() == ServerSuspendController.State.RUNNING) {
            this.service.start();
        }
    }

    @Override
    public void stop() {
        // Only stop if we are not already suspended
        if (this.registry.getState() == ServerSuspendController.State.RUNNING) {
            this.service.stop();
        }
        this.registry.unregisterActivity(this.activity);
    }

    @Override
    public void close() {
        this.service.close();
    }

    @Override
    public Collection<Timer> getTimers() throws EJBException {
        return this.service.getTimers();
    }

    @Override
    public Collection<Timer> getAllTimers() throws EJBException {
        return this.service.getAllTimers();
    }

    @Override
    public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig) throws EJBException {
        return this.service.createSingleActionTimer(expiration, timerConfig);
    }

    @Override
    public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig timerConfig) throws EJBException {
        return this.service.createIntervalTimer(initialExpiration, intervalDuration, timerConfig);
    }

    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig) throws EJBException {
        return this.service.createCalendarTimer(schedule, timerConfig);
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
