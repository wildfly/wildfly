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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.osgi.framework.BundleManagerService;
import org.jboss.osgi.framework.FutureServiceValue;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.Bundle;

/**
 * Abstract base for XService testing.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Oct-2010
 */
abstract class AbstractXServiceTestCase {

    abstract ServiceContainer getServiceContainer();

    @SuppressWarnings("unchecked")
    Bundle registerModule(ModuleIdentifier identifier) throws Exception {

        // Make sure we have an active framework
        ServiceController<Void> frameworkController = (ServiceController<Void>) getServiceContainer().getRequiredService(Services.FRAMEWORK_ACTIVATOR);
        new FutureServiceValue<Void>(frameworkController).get();

        // Get the {@link BundleManagerService}
        ServiceController<BundleManagerService> bundleManagerService = (ServiceController<BundleManagerService>) getServiceContainer().getRequiredService(Services.BUNDLE_MANAGER);
        BundleManagerService bundleManager = bundleManagerService.getValue();

        ServiceController<ModuleLoader> moduleLoaderService = (ServiceController<ModuleLoader>) getServiceContainer().getRequiredService(ServiceName.parse("jboss.as.service-module-loader"));
        ModuleLoader moduleLoader = moduleLoaderService.getValue();
        Module module = moduleLoader.loadModule(identifier);

        ServiceTarget serviceTarget = getServiceContainer().subTarget();
        ServiceName serviceName = bundleManager.registerModule(serviceTarget, module, null);
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
}
