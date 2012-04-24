/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import java.beans.PropertyChangeSupport;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * The overall state of a process that is being managed by a {@link ModelController}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ControlledProcessState {

    public static enum State {
        /**
         * The process is starting and its runtime state is being made consistent with its persistent configuration.
         */
        STARTING("starting"),
        /**
         * The process is started, is running normally and has a runtime state consistent with its persistent configuration.
         */
        RUNNING("running"),
        /**
         * The process requires a stop and re-start of its root service (but not a full process restart) in order to
         * ensure stable operation and/or to bring its running state in line with its persistent configuration. A
         * stop and restart of the root service (also known as a 'reload') will result in the removal of all other
         * services and creation of new services based on the current configuration, so its affect on availability to
         * handle external requests is similar to that of a full process restart. However, a reload can execute more
         * quickly than a full process restart.
         */
        RELOAD_REQUIRED("reload-required"),
        /**
         * The process must be terminated and replaced with a new process in order to ensure stable operation and/or to bring
         * the running state in line with the persistent configuration.
         */
        RESTART_REQUIRED("restart-required"),
        /** The process is stopping. */
        STOPPING("stopping");

        private final String stringForm;

        private State(final String stringForm) {
            this.stringForm = stringForm;
        }

        @Override
        public String toString() {
            return stringForm;
        }

    }

    private final AtomicInteger stamp = new AtomicInteger(0);
    private final AtomicStampedReference<State> state = new AtomicStampedReference<State>(State.STARTING, 0);
    private final boolean reloadSupported;
    private final ControlledProcessStateService service;

    public ControlledProcessState(final boolean reloadSupported) {
        this.reloadSupported = reloadSupported;
        service = new ControlledProcessStateService(State.STARTING);
    }

    public State getState() {
        return state.getReference();
    }

    public boolean isReloadSupported() {
        return reloadSupported;
    }


    public void setStarting() {
        synchronized (service) {
            state.set(State.STARTING, stamp.incrementAndGet());
            service.stateChanged(State.STARTING);
        }
    }

    public void setRunning() {
        synchronized (service) {
            state.set(State.RUNNING, stamp.incrementAndGet());
            service.stateChanged(State.RUNNING);
        }
    }

    public void setStopping() {
        synchronized (service) {
            state.set(State.STOPPING, stamp.incrementAndGet());
            service.stateChanged(State.STOPPING);
        }
    }

    public Object setReloadRequired() {
        if (!reloadSupported) {
            return setRestartRequired();
        }

        AtomicStampedReference<State> stateRef = state;
        int newStamp = stamp.incrementAndGet();
        int[] receiver = new int[1];
        // Keep trying until stateRef is RELOAD_REQUIRED with our stamp
        for (;;) {
            State was = stateRef.get(receiver);
            if (was == State.STARTING || was == State.STOPPING || was == State.RESTART_REQUIRED) {
                break;
            }
            synchronized (service) {
                if (stateRef.compareAndSet(was, State.RELOAD_REQUIRED, receiver[0], newStamp)) {
                    service.stateChanged(State.RELOAD_REQUIRED);
                    break;
                }
            }
        }
        return Integer.valueOf(newStamp);
    }

    public Object setRestartRequired() {
        AtomicStampedReference<State> stateRef = state;
        int newStamp = stamp.incrementAndGet();
        int[] receiver = new int[1];
        // Keep trying until stateRef is RESTART_REQUIRED with our stamp
        for (;;) {
            State was = stateRef.get(receiver);
            if (was == State.STARTING || was == State.STOPPING) {
                break;
            }
            synchronized (service) {
                if (stateRef.compareAndSet(was, State.RESTART_REQUIRED, receiver[0], newStamp)) {
                    service.stateChanged(State.RESTART_REQUIRED);
                    break;
                }
            }
        }
        return Integer.valueOf(newStamp);
    };

    public void revertReloadRequired(Object stamp) {
        if (!reloadSupported) {
            revertRestartRequired(stamp);
        }

        // If 'state' still has the state we last set in restartRequired(), change to RUNNING
        Integer theirStamp = Integer.class.cast(stamp);
        synchronized (service) {
            if (state.compareAndSet(State.RELOAD_REQUIRED, State.RUNNING, theirStamp, this.stamp.incrementAndGet())) {
                service.stateChanged(State.RUNNING);
            }
        }
    }

    public void revertRestartRequired(Object stamp) {
        // If 'state' still has the state we last set in restartRequired(), change to RUNNING
        Integer theirStamp = Integer.class.cast(stamp);
        synchronized (service) {
            if (state.compareAndSet(State.RESTART_REQUIRED, State.RUNNING, theirStamp, this.stamp.incrementAndGet())) {
                service.stateChanged(State.RUNNING);
            }
        }
    }

    ControlledProcessStateService getService() {
        return service;
    }

}
