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

package org.jboss.as.undertow.deployment;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.jboss.as.undertow.Host;
import org.jboss.as.undertow.ServletContainerService;
import org.jboss.as.undertow.UndertowLogger;
import org.jboss.as.undertow.UndertowMessages;
import org.jboss.as.web.common.StartupContext;
import org.jboss.as.web.common.WebInjectionContainer;
import org.jboss.as.web.host.ContextActivator;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 */
public class UndertowDeploymentService implements Service<UndertowDeploymentService> {

    private final InjectedValue<ServletContainerService> container = new InjectedValue<>();
    private final WebInjectionContainer webInjectionContainer;
    private final InjectedValue<Host> host = new InjectedValue<>();
    private final InjectedValue<DeploymentInfo> deploymentInfoInjectedValue = new InjectedValue<>();

    private volatile DeploymentManager deploymentManager;

    public UndertowDeploymentService(final WebInjectionContainer webInjectionContainer) {
        this.webInjectionContainer = webInjectionContainer;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        DeploymentInfo deploymentInfo = deploymentInfoInjectedValue.getValue();

        StartupContext.setInjectionContainer(webInjectionContainer);
        try {
            deploymentManager = container.getValue().getServletContainer().addDeployment(deploymentInfo);
            deploymentManager.deploy();
            try {
                HttpHandler handler = deploymentManager.start();
                host.getValue().registerDeployment(deploymentInfo, handler);
            } catch (ServletException e) {
                throw new StartException(e);
            }
        } finally {
            StartupContext.setInjectionContainer(null);
        }
    }

    @Override
    public void stop(final StopContext stopContext) {
        try {
            deploymentManager.stop();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        deploymentManager.undeploy();
        host.getValue().unregisterDeployment(deploymentInfoInjectedValue.getValue());
    }

    @Override
    public UndertowDeploymentService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<ServletContainerService> getContainer() {
        return container;
    }

    public InjectedValue<Host> getHost() {
        return host;
    }

    public InjectedValue<DeploymentInfo> getDeploymentInfoInjectedValue() {
        return deploymentInfoInjectedValue;
    }


    /**
     * Provides an API to start/stop the {@link UndertowDeploymentService}.
     * This should register/deregister the web context.
     */
    protected static class ContextActivatorImpl implements ContextActivator {

        private final ServiceController<UndertowDeploymentService> controller;


        ContextActivatorImpl(ServiceController<UndertowDeploymentService> controller) {
            this.controller = controller;
        }

        /**
         * Provide access to the Servlet Context.
         */

        /**
         * Start the web context asynchronously.
         * <p/>
         * This would happen during OSGi webapp deployment.
         * <p/>
         * No DUP can assume that all dependencies are available to make a blocking call
         * instead it should call this method.
         */
        public synchronized void startAsync() {
            controller.setMode(ServiceController.Mode.ACTIVE);
        }

        /**
         * Start the web context synchronously.
         * <p/>
         * This would happen when the OSGi webapp gets explicitly started.
         */
        public synchronized boolean start(long timeout, TimeUnit unit) throws TimeoutException {
            if (controller.getMode() == ServiceController.Mode.NEVER) {
                controller.setMode(ServiceController.Mode.ACTIVE);
            }
            final StabilityMonitor monitor = new StabilityMonitor();
            monitor.addController(controller);
            try {
                if (!monitor.awaitStability(timeout, unit)) {
                    throw UndertowMessages.MESSAGES.timeoutContextActivation(controller.getName());
                }
            } catch (final InterruptedException e) {
                // ignore
            } finally {
                monitor.removeController(controller);
            }
            return true;
        }

        /**
         * Stop the web context synchronously.
         * <p/>
         * This would happen when the OSGi webapp gets explicitly stops.
         */
        public synchronized boolean stop(long timeout, TimeUnit unit) {
            boolean result = true;
            if (controller.getMode() == ServiceController.Mode.ACTIVE) {
                controller.setMode(ServiceController.Mode.NEVER);
            }
            final StabilityMonitor monitor = new StabilityMonitor();
            monitor.addController(controller);
            try {
                if (!monitor.awaitStability(timeout, unit)) {
                    UndertowLogger.ROOT_LOGGER.debugf("Timeout stopping context: %s", controller.getName());
                }
            } catch (final InterruptedException e) {
                // ignore
            } finally {
                monitor.removeController(controller);
            }
            return result;
        }

        @Override
        public ServletContext getServletContext() {
            //todo UndertowDeploymentService should be fully started before this method is called
            UndertowDeploymentService service = controller.getValue();
            Deployment deployment = service.deploymentManager.getDeployment();
            return deployment != null ? deployment.getServletContext() : null;
        }
    }
}
