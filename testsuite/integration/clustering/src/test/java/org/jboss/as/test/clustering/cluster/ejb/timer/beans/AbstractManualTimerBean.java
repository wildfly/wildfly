/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import java.util.function.Function;

import jakarta.ejb.NoSuchObjectLocalException;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractManualTimerBean extends AbstractTimerBean implements ManualTimerBean {

    private final Function<TimerService, Timer> factory;

    protected AbstractManualTimerBean(Function<TimerService, Timer> factory) {
        this.factory = factory;
    }

    @Override
    public void createTimer() {
        this.factory.apply(this.service);
        java.util.Collection<Timer> timers = this.service.getTimers();
        assert timers.size() == 1 : timers.toString();
    }

    @Override
    public void cancel() {
        for (Timer timer : this.service.getTimers()) {
            try {
                timer.cancel();
            } catch (NoSuchObjectLocalException e) {
                // This can happen if timer expired concurrently
                // In this case, verify that a fresh call to getTimers() no longer returns it.
                if (this.service.getTimers().contains(timer)) {
                    throw e;
                }
            }
        }
    }

    @Timeout
    public void timeout(Timer timer) {
        this.record(timer);
    }
}
