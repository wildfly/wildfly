/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat Inc., and individual contributors as indicated
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
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.multinode.ejb.timer.database;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.interceptor.Interceptors;

public abstract class RefreshBeanBase implements RefreshIF {
    @Resource
    TimerService timerService;

    @Resource
    SessionContext sessionContext;

    @Timeout
    void timeout(Timer timer) {
        //noop, all timers will be cancelled before expiry.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createTimer(final long delay, final Serializable info) {
        timerService.createSingleActionTimer(delay, new TimerConfig(info, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Interceptors(RefreshInterceptor.class)
    public List<Serializable> getAllTimerInfoWithRefresh() {
        final Collection<Timer> allTimers = timerService.getAllTimers();
        return allTimers.stream().map(Timer::getInfo).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Serializable> getAllTimerInfoWithRefresh2() {
        final RefreshIF businessObject = sessionContext.getBusinessObject(RefreshIF.class);
        return businessObject.getAllTimerInfoWithRefresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Serializable> getAllTimerInfoNoRefresh() {
        return getAllTimerInfoWithRefresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelTimers() {
        final Collection<Timer> timers = timerService.getTimers();
        for (Timer timer : timers) {
            try {
                timer.cancel();
            } catch (Exception e) {
                //ignore
            }
        }
    }
}
