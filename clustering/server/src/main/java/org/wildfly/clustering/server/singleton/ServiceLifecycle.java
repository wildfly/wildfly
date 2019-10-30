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
import java.util.Map;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;

/**
 * Starts/stops a given {@link ServiceController}.
 * @author Paul Ferraro
 */
public class ServiceLifecycle implements Lifecycle {
    // Mapping of service controller mode changes that appropriate for toggling to a given controller state
    private static final Map<State, Map<Mode, Mode>> modeToggle = new EnumMap<>(State.class);
    static {
        Map<Mode, Mode> map = new EnumMap<>(Mode.class);
        map.put(Mode.NEVER, Mode.ACTIVE);
        map.put(Mode.ON_DEMAND, Mode.PASSIVE);
        modeToggle.put(State.UP, map);

        map = new EnumMap<>(Mode.class);
        map.put(Mode.ACTIVE, Mode.NEVER);
        map.put(Mode.PASSIVE, Mode.ON_DEMAND);
        modeToggle.put(State.DOWN, map);
    }

    private final ServiceController<?> controller;

    public ServiceLifecycle(ServiceController<?> controller) {
        this.controller = controller;
    }

    @Override
    public void start() {
        this.transition(State.UP);
    }

    @Override
    public void stop() {
        this.transition(State.DOWN);
    }

    private void transition(State targetState) {
        // Short-circuit if the service is already at the target state
        if (this.controller.getState() == targetState) return;

        StabilityMonitor monitor = new StabilityMonitor();
        monitor.addController(this.controller);
        try {
            // Force service to transition to desired state
            Mode targetMode = modeToggle.get(targetState).get(this.controller.getMode());
            if (targetMode != null) {
                this.controller.setMode(targetMode);
            }

            monitor.awaitStability();

            if (this.controller.getState() == ServiceController.State.START_FAILED) {
                throw new IllegalStateException(this.controller.getStartException());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            monitor.removeController(this.controller);
        }
    }
}
