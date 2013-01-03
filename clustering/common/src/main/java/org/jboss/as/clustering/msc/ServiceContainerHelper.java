/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.msc;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceController.Transition;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.StartException;

/**
 * Helper methods for interacting with a modular service container.
 * @author Paul Ferraro
 */
public class ServiceContainerHelper {
    private static Logger log = Logger.getLogger(ServiceContainerHelper.class);

    // Mapping of service controller mode changes that appropriate for toggling to a given controller state
    private static final Map<State, Map<Mode, Mode>> modeToggle = new EnumMap<State, Map<Mode, Mode>>(State.class);
    static {
        Map<Mode, Mode> map = new EnumMap<Mode, Mode>(Mode.class);
        map.put(Mode.NEVER, Mode.ACTIVE);
        map.put(Mode.ON_DEMAND, Mode.PASSIVE);
        modeToggle.put(State.UP, map);

        map = new EnumMap<Mode, Mode>(Mode.class);
        map.put(Mode.ACTIVE, Mode.NEVER);
        map.put(Mode.PASSIVE, Mode.ON_DEMAND);
        modeToggle.put(State.DOWN, map);

        map = new EnumMap<Mode, Mode>(Mode.class);
        for (Mode mode: Mode.values()) {
            if (mode != Mode.REMOVE) {
                map.put(mode, Mode.REMOVE);
            }
        }
        modeToggle.put(State.REMOVED, map);
    }

    /**
     * Returns the current service container.
     * @return a service container
     */
    public static ServiceContainer getCurrentServiceContainer() {
        PrivilegedAction<ServiceContainer> action = new PrivilegedAction<ServiceContainer>() {
            @Override
            public ServiceContainer run() {
                return CurrentServiceContainer.getServiceContainer();
            }
        };
        return AccessController.doPrivileged(action);
    }

    /**
     * Returns the service value of the specified service, starting it if necessary.
     * @param controller a service controller
     * @param targetClass the service value class
     * @return the service value of the specified service
     * @throws StartException if the specified service could not be started
     */
    public static <T> T getValue(ServiceController<?> controller, Class<T> targetClass) throws StartException {
        return targetClass.cast(getValue(controller));
    }

    /**
     * Returns the service value of the specified service, starting it if necessary.
     * @param controller a service controller
     * @return the service value of the specified service
     * @throws StartException if the specified service could not be started
     */
    public static <T> T getValue(ServiceController<T> controller) throws StartException {
        start(controller);
        return controller.getValue();
    }

    /**
     * Ensures the specified service is started.
     * @param controller a service controller
     * @throws StartException if the specified service could not be started
     */
    public static void start(final ServiceController<?> controller) throws StartException {
        transition(controller, State.UP);
    }

    /**
     * Ensures the specified service is stopped.
     * @param controller a service controller
     */
    public static void stop(ServiceController<?> controller) {
        try {
            transition(controller, State.DOWN);
        } catch (StartException e) {
            // This can't happen
            throw new IllegalStateException(e);
        }
    }

    /**
     * Ensures the specified service is removed.
     * @param controller a service controller
     */
    public static void remove(ServiceController<?> controller) {
        try {
            transition(controller, State.REMOVED);
        } catch (StartException e) {
            // This can't happen
            throw new IllegalStateException(e);
        }
    }

    private static void transition(final ServiceController<?> targetController, State targetState) throws StartException {
        // Short-circuit if the service is already at the target state
        if (targetController.getState() == targetState) return;

        // Track any services installed by the target service
        final Queue<ServiceController<?>> controllers = new ConcurrentLinkedQueue<ServiceController<?>>(Collections.singleton(targetController));
        final ServiceListener<Object> listener = new AbstractServiceListener<Object>() {
            @Override
            public void transition(ServiceController<? extends Object> controller, Transition transition) {
                log.tracef("%s transitioned from %s", controller.getName(), transition);
                if (transition.leavesRestState()) {
                    // Target controller is already in queue
                    if (controller != targetController) {
                        controllers.add(controller);
                    }
                } else if (transition.entersRestState()) {
                    synchronized (controller) {
                        controller.notify();
                    }
                }
            }
        };
        targetController.addListener(ServiceListener.Inheritance.ALL, listener);
        try {
            if (targetController.getSubstate().isRestState()) {
                // Force service to transition to desired state
                Mode targetMode = modeToggle.get(targetState).get(targetController.getMode());
                if (targetMode != null) {
                    targetController.setMode(targetMode);
                }
            }
            while (!controllers.isEmpty()) {
                ServiceController<?> controller = controllers.remove();
                synchronized (controller) {
                    if (!controller.getSubstate().isRestState()) {
                        // Listener will notify us when we enter rest state
                        controller.wait();
                    }
                }
                if (targetState == State.UP) {
                    StartException exception = controller.getStartException();
                    if (exception != null) {
                        throw exception;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            targetController.removeListener(listener);
        }
    }

    private ServiceContainerHelper() {
        // Hide
    }
}
