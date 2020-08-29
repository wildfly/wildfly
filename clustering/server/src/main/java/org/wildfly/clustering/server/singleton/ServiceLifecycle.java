/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.singleton;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.management.ServiceNotFoundException;

import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.service.CountDownLifecycleListener;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;

/**
 * Starts/stops a given {@link ServiceController}.
 * @author Paul Ferraro
 */
public class ServiceLifecycle implements Lifecycle {
    private enum Transition {
        START(EnumSet.of(State.UP, State.START_FAILED, State.REMOVED), LifecycleEvent.DOWN),
        STOP(EnumSet.of(State.DOWN, State.REMOVED), LifecycleEvent.UP),
        ;
        Set<State> targetStates;
        Set<LifecycleEvent> targetEvents;
        Map<Mode, Mode> modeTransitions;

        Transition(Set<State> targetStates, LifecycleEvent sourceEvent) {
            this.targetStates = targetStates;
            this.targetEvents = EnumSet.complementOf(EnumSet.of(sourceEvent));
            this.modeTransitions = new EnumMap<>(Mode.class);
            boolean up = this.targetStates.contains(State.UP);
            this.modeTransitions.put(up ? Mode.NEVER : Mode.ACTIVE, up ? Mode.ACTIVE : Mode.NEVER);
            this.modeTransitions.put(up ? Mode.ON_DEMAND : Mode.PASSIVE, up ? Mode.PASSIVE : Mode.ON_DEMAND);
        }
    }

    private final ServiceController<?> controller;

    public ServiceLifecycle(ServiceController<?> controller) {
        this.controller = controller;
    }

    @Override
    public void start() {
        this.transition(Transition.START);
    }

    @Override
    public void stop() {
        this.transition(Transition.STOP);
    }

    private void transition(Transition transition) {
        // Short-circuit if the service is already at the target state
        if (this.isComplete(transition)) return;

        CountDownLatch latch = new CountDownLatch(1);
        LifecycleListener listener = new CountDownLifecycleListener(latch, transition.targetEvents);
        this.controller.addListener(listener);

        try {
            if (this.isComplete(transition)) return;

            // Force service to transition to desired state
            Mode currentMode = this.controller.getMode();
            if (currentMode == ServiceController.Mode.REMOVE) {
                throw new IllegalStateException(new ServiceNotFoundException(this.controller.getName().getCanonicalName()));
            }
            Mode targetMode = transition.modeTransitions.get(currentMode);
            if (targetMode == null) {
                throw new IllegalStateException(currentMode.name());
            }

            this.controller.setMode(targetMode);

            latch.await();

            if (this.controller.getState() == State.START_FAILED) {
                throw new IllegalStateException(this.controller.getStartException());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.controller.removeListener(listener);
        }
    }

    private boolean isComplete(Transition transition) {
        State state = this.controller.getState();
        if (transition.targetStates.contains(state)) {
            if (state == State.START_FAILED) {
                throw new IllegalStateException(this.controller.getStartException());
            }
            return true;
        }
        return false;
    }
}
