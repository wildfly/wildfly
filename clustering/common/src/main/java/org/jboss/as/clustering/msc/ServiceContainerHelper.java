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
import java.util.Collection;
import java.util.EnumSet;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceController.Transition;
import org.jboss.msc.service.StartException;

/**
 * Helper methods for interacting with a modular service container.
 * @author Paul Ferraro
 */
public class ServiceContainerHelper {
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
    public static void start(ServiceController<?> controller) throws StartException {
        // If service is down, set the mode appropriately so the controller will start it
        if (controller.getState() == ServiceController.State.DOWN) {
            switch (controller.getMode()) {
                case NEVER: {
                    controller.setMode(ServiceController.Mode.ACTIVE);
                    break;
                }
                case ON_DEMAND: {
                    controller.setMode(ServiceController.Mode.PASSIVE);
                    break;
                }
                default: {
                    // Do nothing
                }
            }
        }
        if (!wait(controller, EnumSet.of(ServiceController.State.DOWN, ServiceController.State.STARTING), ServiceController.State.UP)) {
            throw controller.getStartException();
        }
    }

    /**
     * Ensures the specified service is stopped.
     * @param controller a service controller
     */
    public static void stop(ServiceController<?> controller) {
        // If service is up, set the mode appropriately so the controller will stop it
        if (controller.getState() == ServiceController.State.UP) {
            switch (controller.getMode()) {
                case ACTIVE: {
                    controller.setMode(ServiceController.Mode.NEVER);
                    break;
                }
                case PASSIVE: {
                    controller.setMode(ServiceController.Mode.ON_DEMAND);
                    break;
                }
                default: {
                    // Do nothing
                }
            }
        }
        wait(controller, EnumSet.of(ServiceController.State.UP, ServiceController.State.STOPPING), ServiceController.State.DOWN);
    }

    /**
     * Ensures the specified service is removed.
     * @param controller a service controller
     */
    public static void remove(ServiceController<?> controller) {
        controller.setMode(ServiceController.Mode.REMOVE);
        wait(controller, EnumSet.of(ServiceController.State.UP, ServiceController.State.STOPPING, ServiceController.State.DOWN), ServiceController.State.REMOVED);
    }

    private static <T> boolean wait(ServiceController<T> controller, Collection<ServiceController.State> expectedStates, ServiceController.State targetState) {
        if (controller.getState() == targetState) return true;
        ServiceListener<T> listener = new NotifyingServiceListener<T>();
        controller.addListener(listener);
        try {
            synchronized (controller) {
                while (expectedStates.contains(controller.getState())) {
                    controller.wait();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        controller.removeListener(listener);
        return controller.getState() == targetState;
    }

    private static class NotifyingServiceListener<T> extends AbstractServiceListener<T> {
        @Override
        public void transition(ServiceController<? extends T> controller, Transition transition) {
            synchronized (controller) {
                controller.notify();
            }
        }
    }

    private ServiceContainerHelper() {
        // Hide
    }
}
