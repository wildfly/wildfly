/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.timer.passivation.bean;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

/**
 * A singleton session bean that manages timers with serializable info objects.
 * This bean is used to test that timer info (serializable objects) are properly
 * persisted and retrieved by the distributable timer service.
 *
 * @author Radoslav Husar
 */
@Singleton
@Remote(PassivationTimeoutCollector.class)
public class PassivationTimeoutCollectorBean implements PassivationTimeoutCollector {
    static final Deque<Instant> ACTIVATIONS = new LinkedBlockingDeque<>();
    static final Deque<Instant> PASSIVATIONS = new LinkedBlockingDeque<>();

    @Resource
    private TimerService timerService;
    private final Deque<Instant> timeouts = new LinkedBlockingDeque<>();

    @PostConstruct
    public void init() {
        this.timerService.createIntervalTimer(TIMEOUT_INTERVAL.toMillis(), TIMEOUT_INTERVAL.toMillis(), new TimerConfig(new TimerInfo(), false));
        // Create a second timer with a shorter interval to encourage passivation of the first
        this.timerService.createIntervalTimer(0L, TIMEOUT_INTERVAL.dividedBy(10L).toMillis(), new TimerConfig("", false));
    }

    @Timeout
    public void timeout(Timer timer) {
        if (timer.getInfo() instanceof TimerInfo) {
            System.out.println("@Timeout at " + Instant.now());
            this.timeouts.add(Instant.now());
        }
    }

    @Override
    public Deque<Instant> getTimeouts() {
        return this.timeouts;
    }

    @Override
    public Deque<Instant> getPassivations() {
        return PASSIVATIONS;
    }

    @Override
    public Deque<Instant> getActivations() {
        return ACTIVATIONS;
    }
}
