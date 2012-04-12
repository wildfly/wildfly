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

package org.jboss.as.osgi.deployment;

import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.PackageAdmin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.service.FrameworkBootstrapService.FRAMEWORK_BASE_NAME;
import static org.osgi.service.packageadmin.PackageAdmin.BUNDLE_TYPE_FRAGMENT;

/**
 * A service that collects installed bundle services and starts them when all collected bundles were installed in the framework.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 30-Mar-2011
 */
public class BundleStartTracker implements Service<BundleStartTracker> {

    public static final ServiceName SERVICE_NAME = FRAMEWORK_BASE_NAME.append("starttracker");

    private final InjectedValue<PackageAdmin> injectedPackageAdmin = new InjectedValue<PackageAdmin>();
    private final Map<ServiceName, Tuple> pendingServices = new ConcurrentHashMap<ServiceName, Tuple>();
    private final Map<ServiceName, Tuple> startedServices = new ConcurrentHashMap<ServiceName, Tuple>();
    private volatile ServiceContainer serviceContainer;

    public static final ServiceController<?> addService(ServiceTarget serviceTarget) {
        BundleStartTracker service = new BundleStartTracker();
        ServiceBuilder<BundleStartTracker> builder = serviceTarget.addService(SERVICE_NAME, service);
        builder.addDependency(Services.PACKAGE_ADMIN, PackageAdmin.class, service.injectedPackageAdmin);
        builder.setInitialMode(Mode.PASSIVE);
        return builder.install();
    }

    private BundleStartTracker() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        LOGGER.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        serviceContainer = context.getController().getServiceContainer();
    }

    @Override
    public void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        LOGGER.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
    }

    @Override
    public BundleStartTracker getValue() throws IllegalStateException {
        return this;
    }

    @SuppressWarnings("unchecked")
    void addInstalledBundle(ServiceName serviceName, Deployment deployment) {
        ServiceController<Bundle> controller = (ServiceController<Bundle>) serviceContainer.getRequiredService(serviceName);
        pendingServices.put(serviceName, new Tuple(controller, deployment));
        controller.addListener(new AbstractServiceListener<Bundle>() {

            @Override
            public void listenerAdded(ServiceController<? extends Bundle> controller) {
                State state = controller.getState();
                if (state == State.UP || state == State.START_FAILED)
                    processService(controller);
            }

            @Override
            public void transition(final ServiceController<? extends Bundle> controller, final ServiceController.Transition transition) {
                if (transition.getBefore() == ServiceController.Substate.STARTING) {
                    switch (transition.getAfter()) {
                        case UP:
                            ServiceName key = controller.getName();
                            Tuple value = pendingServices.get(key);
                            startedServices.put(key, value);
                            // fall thru
                        case START_FAILED:
                            processService(controller);
                    }
                }
            }

            private void processService(ServiceController<? extends Bundle> controller) {
                controller.removeListener(this);
                Map<ServiceName, Tuple> bundlesToStart = null;
                synchronized (this) {
                    ServiceName key = controller.getName();
                    pendingServices.remove(key);
                    if (pendingServices.isEmpty()) {
                        bundlesToStart = new HashMap<ServiceName, Tuple>(startedServices);
                        startedServices.clear();
                    }
                }
                if (bundlesToStart != null) {
                    PackageAdmin packageAdmin = injectedPackageAdmin.getValue();
                    for (Tuple tuple : bundlesToStart.values()) {
                        Bundle bundle = tuple.controller.getValue();
                        Deployment dep = tuple.deployment;
                        if (dep.isAutoStart()) {
                            try {
                                int bundleType = packageAdmin.getBundleType(bundle);
                                if (bundleType != BUNDLE_TYPE_FRAGMENT) {
                                    bundle.start(Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY);
                                }
                            } catch (BundleException ex) {
                                LOGGER.errorCannotStartBundle(ex, bundle);
                            }
                        }
                    }
                }
            }
        });
    }

    private static final class Tuple {
        private ServiceController<? extends Bundle> controller;
        private Deployment deployment;

        Tuple(ServiceController<? extends Bundle> controller, Deployment deployment) {
            this.controller = controller;
            this.deployment = deployment;
        }
    }
}
