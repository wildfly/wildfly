/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.testsuite.integration.osgi.xservice;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.framework.BundleManagerService;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;

/**
 * Abstract base for XService testing.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Oct-2010
 */
abstract class AbstractXServiceTestCase {

    private static final Logger log = Logger.getLogger(AbstractXServiceTestCase.class);

    abstract ServiceContainer getServiceContainer();

    @SuppressWarnings("unchecked")
    Bundle registerModule(ModuleIdentifier identifier) throws Exception {

        // Make sure we have an active framework
        ServiceController<Framework> frameworkController = (ServiceController<Framework>) getServiceContainer().getRequiredService(Services.FRAMEWORK_ACTIVE);
        new FutureServiceValue<Framework>(frameworkController).get();

        // Get the {@link BundleManagerService}
        ServiceController<BundleManagerService> bundleManagerService = (ServiceController<BundleManagerService>) getServiceContainer().getRequiredService(Services.BUNDLE_MANAGER);
        BundleManagerService bundleManager = bundleManagerService.getValue();

        ServiceController<ModuleLoader> moduleLoaderService = (ServiceController<ModuleLoader>) getServiceContainer().getRequiredService(ServiceName.parse("jboss.as.service-module-loader"));
        ModuleLoader moduleLoader = moduleLoaderService.getValue();
        Module module = moduleLoader.loadModule(identifier);

        ServiceTarget serviceTarget = getServiceContainer().subTarget();
        ServiceName serviceName = bundleManager.installBundle(serviceTarget, module, null);
        return getBundleFromService(serviceName);
    }

    @SuppressWarnings("unchecked")
    private Bundle getBundleFromService(ServiceName serviceName) throws ExecutionException, TimeoutException {
        ServiceController<Bundle> controller = (ServiceController<Bundle>) getServiceContainer().getService(serviceName);
        FutureServiceValue<Bundle> future = new FutureServiceValue<Bundle>(controller);
        return future.get(5, TimeUnit.SECONDS);
    }

    void assertServiceState(ServiceName serviceName, State expState, long timeout) throws Exception {
        ServiceController<?> controller = getServiceContainer().getService(serviceName);
        State state = controller != null ? controller.getState() : null;
        while ((state == null || state != expState) && timeout > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // ignore
            }
            controller = getServiceContainer().getService(serviceName);
            state = controller != null ? controller.getState() : null;
            timeout -= 100;
        }
        assertEquals(serviceName.toString(), expState, state);
    }

    private static final class FutureServiceValue<T> implements Future<T> {

        private ServiceController<T> controller;

        FutureServiceValue(ServiceController<T> controller) {
            this.controller = controller;
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
            return controller.getState() == State.UP;
        }

        @Override
        public T get() throws ExecutionException {
            try {
                return get(5, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                throw new ExecutionException(ex);
            }
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
            return getValue(timeout, unit);
        }

        private T getValue(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {

            if (controller.getState() == State.UP)
                return controller.getValue();

            final CountDownLatch latch = new CountDownLatch(1);
            AbstractServiceListener<T> listener = new AbstractServiceListener<T>() {

                @Override
                public void listenerAdded(ServiceController<? extends T> controller) {
                    State state = controller.getState();
                    if (state == State.UP || state == State.START_FAILED)
                        listenerDone(controller);
                }

                @Override
                public void serviceStarted(ServiceController<? extends T> controller) {
                    listenerDone(controller);
                }

                @Override
                public void serviceFailed(ServiceController<? extends T> controller, StartException reason) {
                    listenerDone(controller);
                }

                private void listenerDone(ServiceController<? extends T> controller) {
                    controller.removeListener(this);
                    latch.countDown();
                }
            };

            controller.addListener(listener);

            try {
                if (latch.await(timeout, unit) == false) {
                    TimeoutException cause = new TimeoutException("Timeout getting " + controller.getName());
                    processExceptionCause(cause);
                    throw cause;
                }
            } catch (InterruptedException e) {
                // ignore;
            }

            if (controller.getState() == State.UP)
                return controller.getValue();

            StartException startException = controller.getStartException();
            Throwable cause = (startException != null ? startException.getCause() : null);
            String message = processExceptionCause(cause);

            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            if (cause instanceof ExecutionException)
                throw (ExecutionException) cause;
            if (cause instanceof TimeoutException)
                throw (TimeoutException) cause;

            throw new ExecutionException(message, cause);
        }

        String processExceptionCause(Throwable cause) throws ExecutionException, TimeoutException {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            controller.getServiceContainer().dumpServices(new PrintStream(baos));
            String message = "Cannot start " + controller.getName();
            log.debugf(message + "\n%s", baos.toString());
            log.errorf(message);
            return message;
        }
    }
}
