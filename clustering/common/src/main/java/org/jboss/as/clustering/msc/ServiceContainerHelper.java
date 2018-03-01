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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.StartException;

/**
 * Helper methods for interacting with a modular service container.
 * @author Paul Ferraro
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ServiceContainerHelper {
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

        map = new EnumMap<>(Mode.class);
        for (Mode mode: EnumSet.complementOf(EnumSet.of(Mode.REMOVE))) {
            map.put(mode, Mode.REMOVE);
        }
        modeToggle.put(State.REMOVED, map);
    }

    /**
     * Returns the value of the specified service, if the service exists and is started.
     * @param registry the service registry
     * @param name the service name
     * @return the service value, if the service exists and is started, null otherwise
     */
    public static <T> T findValue(ServiceRegistry registry, ServiceName name) {
        ServiceController<T> service = findService(registry, name);
        return ((service != null) && (service.getState() == State.UP)) ? service.getValue() : null;
    }

    /**
     * Generics friendly version of {@link ServiceRegistry#getService(ServiceName)}
     * @param registry service registry
     * @param name service name
     * @return the service controller with the specified name, or null if the service does not exist
     */
    @SuppressWarnings("unchecked")
    public static <T> ServiceController<T> findService(ServiceRegistry registry, ServiceName name) {
        return (ServiceController<T>) registry.getService(name);
    }

    /**
     * Generics friendly version of {@link ServiceRegistry#getRequiredService(ServiceName)}
     * @param registry service registry
     * @param name service name
     * @return the service controller with the specified name
     * @throws org.jboss.msc.ServiceNotFoundException if the service was not found
     */
    @SuppressWarnings("unchecked")
    public static <T> ServiceController<T> getService(ServiceRegistry registry, ServiceName name) {
        return (ServiceController<T>) registry.getRequiredService(name);
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
    public static <T> void start(final ServiceController<T> controller) throws StartException {
        transition(controller, State.UP);

        StartException exception = controller.getStartException();
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Ensures the specified service is stopped.
     * @param controller a service controller
     */
    public static <T> void stop(ServiceController<T> controller) {
        transition(controller, State.DOWN);
    }

    /**
     * Ensures the specified service is removed.
     * @param controller a service controller
     */
    public static <T> void remove(ServiceController<T> controller) {
        transition(controller, State.REMOVED);
    }

    private static <T> void transition(final ServiceController<T> targetController, final State targetState) {
        // Short-circuit if the service is already at the target state
        if (targetController.getState() == targetState) return;

        StabilityMonitor monitor = new StabilityMonitor();
        monitor.addController(targetController);
        try {
            // Force service to transition to desired state
            Mode targetMode = modeToggle.get(targetState).get(targetController.getMode());
            if (targetMode != null) {
                targetController.setMode(targetMode);
            }

            monitor.awaitStability();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            monitor.removeController(targetController);
        }
    }

    private ServiceContainerHelper() {
        // Hide
    }
}
