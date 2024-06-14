/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.BiFunction;

import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

/**
 * @author Paul Ferraro
 */
public class BurstPersistentTimerBean extends AbstractManualTimerBean {
    public static final Duration START_DURATION = Duration.ofSeconds(5);
    public static final Duration BURST_DURATION = Duration.ofSeconds(5);

    public BurstPersistentTimerBean(BiFunction<TimerService, Map.Entry<Instant, Instant>, Timer> timerFactory) {
        // Fire every second for 5 seconds after waiting 5 seconds
        super(service -> {
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Instant start = now.plus(START_DURATION);
            Instant end = start.plus(BURST_DURATION);
            return timerFactory.apply(service, Map.entry(start, end));
        });
    }
}
