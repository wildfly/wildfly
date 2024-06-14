/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;

/**
 * @author Paul Ferraro
 */
@Singleton
@Startup
@Local(ManualTimerBean.class)
public class BurstPersistentIntervalTimerBean extends BurstPersistentTimerBean {

    public BurstPersistentIntervalTimerBean() {
        // Fire every second for 5 seconds after waiting 10 seconds
        super((service, entry) -> {
            return service.createIntervalTimer(Date.from(entry.getKey()), 1000L, new TimerConfig(new TimerInfo(entry.getValue()), true));
        });
    }

    @Timeout
    @Override
    public void timeout(Timer timer) {
        super.timeout(timer);
        TimerInfo info = (TimerInfo) timer.getInfo();
        if (!Instant.now().isBefore(info.end)) {
            timer.cancel();
        }
    }

    private static class TimerInfo implements Serializable {
        private static final long serialVersionUID = -4612974784446686670L;

        private final Instant end;

        TimerInfo(Instant end) {
            this.end = end;
        }

        @Override
        public String toString() {
            return BurstPersistentIntervalTimerBean.class.getName();
        }
    }
}
