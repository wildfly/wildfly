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

package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.StartException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;
import static org.jboss.as.connector.logging.ConnectorMessages.MESSAGES;

/**
 * A Future that waits for the given service to come up and returns it's value.
 *
 * Use cautiously and only if there is no way to use direct service dependencies instead.
 *
 * @author Stefano Maestri
 * taken from osgi by thomas.diesler@jboss.com
 */
public final class FutureServiceValue<T> implements Future<T> {

    private final ServiceController<T> controller;
    private final ServiceController.State expectedState;

    public FutureServiceValue(ServiceController<T> controller) {
        this(controller, ServiceController.State.UP);
    }

    public FutureServiceValue(ServiceController<T> controller, ServiceController.State state) {
        if (controller == null)
            throw MESSAGES.illegalArgumentNull("controller");
        if (state == null)
            throw MESSAGES.illegalArgumentNull("state");
        this.controller = controller;
        this.expectedState = state;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return controller.getState() == expectedState;
    }

    @Override
    public T get() throws ExecutionException {
        return getValue();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
        return getValue(timeout, unit);
    }

    private T getValue(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {

        if (controller.getState() == expectedState)
            return controller.getValue();

        final CountDownLatch latch = new CountDownLatch(1);
        final String serviceName = controller.getName().getCanonicalName();
        ServiceListener<T> listener = getServiceListener(latch);

        controller.addListener(listener);
        try {
            if (latch.await(timeout, unit) == false) {
                TimeoutException ex = MESSAGES.timeoutGettingService(serviceName);
                throw ex;
            }
        } catch (InterruptedException e) {
            // ignore
        } finally {
            controller.removeListener(listener);
        }

        if (controller.getState() == expectedState)
            return expectedState == ServiceController.State.UP ? controller.getValue() : null;

        Throwable cause = controller.getStartException();
        while (cause instanceof StartException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        throw MESSAGES.cannotGetServiceValue(cause, serviceName);
    }

    private T getValue() throws ExecutionException {

            if (controller.getState() == expectedState)
                return controller.getValue();

            final CountDownLatch latch = new CountDownLatch(1);
            final String serviceName = controller.getName().getCanonicalName();
            ServiceListener<T> listener = getServiceListener(latch);

            controller.addListener(listener);
            try {
             latch.await();

            } catch (InterruptedException e) {
                // ignore
            } finally {
                controller.removeListener(listener);
            }

            if (controller.getState() == expectedState)
                return expectedState == ServiceController.State.UP ? controller.getValue() : null;

            Throwable cause = controller.getStartException();
            while (cause instanceof StartException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw MESSAGES.cannotGetServiceValue(cause, serviceName);
        }

    private ServiceListener<T> getServiceListener(final CountDownLatch latch) {
        final FutureServiceValue<T> futureServiceValue = this;
        final String serviceName = controller.getName().getCanonicalName();

        return new AbstractServiceListener<T>() {

                @Override
                public void listenerAdded(ServiceController<? extends T> controller) {
                    ServiceController.State state = controller.getState();
                    if (state == expectedState || state == ServiceController.State.START_FAILED)
                        listenerDone(controller);
                }

                @Override
                public void transition(final ServiceController<? extends T> controller, final ServiceController.Transition transition) {
                    ROOT_LOGGER.tracef("transition %s %s => %s", futureServiceValue, serviceName, transition);
                    ServiceController.Substate targetState = transition.getAfter();
                    switch (expectedState) {
                        case UP:
                            if (targetState == ServiceController.Substate.UP || targetState == ServiceController.Substate.START_FAILED) {
                                listenerDone(controller);
                            }
                            break;
                        case DOWN:
                            if (targetState == ServiceController.Substate.DOWN) {
                                listenerDone(controller);
                            }
                            break;
                        case REMOVED:
                            if (targetState == ServiceController.Substate.REMOVED) {
                                listenerDone(controller);
                            }
                            break;
                    }
                }

                private void listenerDone(ServiceController<? extends T> controller) {
                    latch.countDown();
                }
            };
    }
}
