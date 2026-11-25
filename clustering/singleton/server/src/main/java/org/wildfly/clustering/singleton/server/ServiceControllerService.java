/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.server.service.Service;
import org.wildfly.clustering.service.CountDownLifecycleListener;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceNotFoundException;

/**
 * Starts/stops a given {@link ServiceController}.
 * @author Paul Ferraro
 */
public class ServiceControllerService implements Service {
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

    public ServiceControllerService(ServiceController<?> controller) {
        this.controller = controller;
    }

    @Override
    public boolean isStarted() {
        return this.controller.getState() == ServiceController.State.UP;
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
                throw new IllegalStateException(new ServiceNotFoundException(this.controller.toString()));
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
