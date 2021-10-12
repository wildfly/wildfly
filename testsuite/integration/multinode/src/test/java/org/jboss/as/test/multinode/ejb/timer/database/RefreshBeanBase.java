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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.interceptor.Interceptors;

public abstract class RefreshBeanBase implements RefreshIF {
    @Resource
    TimerService timerService;

    @Resource
    SessionContext sessionContext;

    @SuppressWarnings("unused")
    @Timeout
    void timeout(Timer timer) {
        //noop, all timers will be cancelled before expiry.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] createTimer(final long delay, final Serializable info) {
        final Timer timer = timerService.createSingleActionTimer(delay, new TimerConfig(info, true));

        if (info == Info.RETURN_HANDLE) {
            final TimerHandle handle = timer.getHandle();
            try (final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 final ObjectOutputStream out = new ObjectOutputStream(bos)) {
                out.writeObject(handle);
                out.flush();
                return bos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize timer handle for timer: " + timer, e);
            }
        } else {
            return null;
        }
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelTimer(final byte[] handle) {
        final TimerHandle timerHandle;
        try (final ByteArrayInputStream bis = new ByteArrayInputStream(handle);
             final ObjectInputStream in = new ObjectInputStream(bis)) {
            timerHandle = (TimerHandle) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize byte[] to timer handle.", e);
        }
        final Timer timer = timerHandle.getTimer();
        if (timer != null) {
            timer.cancel();
        } else {
            throw new RuntimeException("Failed to get timer from timer handle byte[].");
        }
    }
}
