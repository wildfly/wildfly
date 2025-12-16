/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.timer.passivation.bean;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import org.jboss.as.test.clustering.PassivationEventTrackerBean;

/**
 * A singleton session bean that manages timers with serializable info objects.
 * This bean is used to test that timer info (serializable objects) are properly
 * persisted and retrieved by the distributable timer service.
 *
 * @author Radoslav Husar
 */
@Startup
@Singleton
@Remote(TimerTracker.class)
public class TimerTrackerBean extends PassivationEventTrackerBean implements TimerTracker {

    @Resource
    private TimerService timerService;
    private final LinkedBlockingQueue<Timer> timerQueue = new LinkedBlockingQueue<>();

    @Override
    public void createTimer(String name, boolean persistent, Duration duration) {
        TimerInfo info = new TimerInfo(name);
        TimerConfig config = new TimerConfig(info, persistent);
        Timer timer = this.timerService.createSingleActionTimer(duration.toMillis(), config);
        timerQueue.add(timer);

        System.out.printf("Created timer on server with info: %s, persistent? %s, duration %d ms.%n", info, persistent, duration.toMillis());
    }

    @Timeout
    public void timeout(Timer timer) {
        System.out.println("@Timeout fired for timer with info: " + timer.getInfo());
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("Called @PreDestroy - removing created timers");

        Stream.generate(timerQueue::poll)
            .takeWhile(Objects::nonNull)
            .forEach(timer -> {
                try {
                    timer.cancel();
                } catch (RuntimeException rex) {
                    System.out.println("Encountered exception while cancelling timer - Timer may have already been canceled (e.g., the single-action timer already fired)");
                }
            });
    }
}
