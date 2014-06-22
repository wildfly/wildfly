/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import java.io.File;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.jboss.as.web.common.StartupContext;
import org.jboss.as.web.common.WebInjectionContainer;
import org.jboss.as.web.host.ContextActivator;
import org.jboss.el.cache.FactoryFinderCache;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Stuart Douglas
 */
public class UndertowDeploymentService implements Service<UndertowDeploymentService> {

    private final InjectedValue<ServletContainerService> container = new InjectedValue<>();
    private final WebInjectionContainer webInjectionContainer;
    private final InjectedValue<Host> host = new InjectedValue<>();
    private final InjectedValue<DeploymentInfo> deploymentInfoInjectedValue = new InjectedValue<>();
    private final boolean autostart;

    private volatile DeploymentManager deploymentManager;

    public UndertowDeploymentService(final WebInjectionContainer webInjectionContainer, boolean autostart) {
        this.webInjectionContainer = webInjectionContainer;
        this.autostart = autostart;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        if (autostart) {
            try {
                startContext();
            } catch (ServletException e) {
                throw new StartException(e);
            }
        }
    }

    public void startContext() throws ServletException {
        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        DeploymentInfo deploymentInfo = deploymentInfoInjectedValue.getValue();
        Thread.currentThread().setContextClassLoader(deploymentInfo.getClassLoader());
        try {
            StartupContext.setInjectionContainer(webInjectionContainer);
            try {
                deploymentManager = container.getValue().getServletContainer().addDeployment(deploymentInfo);
                deploymentManager.deploy();
                HttpHandler handler = deploymentManager.start();
                Deployment deployment = deploymentManager.getDeployment();
                host.getValue().registerDeployment(deployment, handler);
            } finally {
                StartupContext.setInjectionContainer(null);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public void stop(final StopContext stopContext) {
        stopContext();
    }

    public void stopContext() {

        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        DeploymentInfo deploymentInfo = deploymentInfoInjectedValue.getValue();
        Thread.currentThread().setContextClassLoader(deploymentInfo.getClassLoader());
        try {
            FactoryFinderCache.clearClassLoader(deploymentInfo.getClassLoader());
            if (deploymentManager != null) {
                Deployment deployment = deploymentManager.getDeployment();
                try {
                    host.getValue().unregisterDeployment(deployment);
                    deploymentManager.stop();
                } catch (ServletException e) {
                    throw new RuntimeException(e);
                }
                deploymentManager.undeploy();
                container.getValue().getServletContainer().removeDeployment(deploymentInfoInjectedValue.getValue());
            }
            recursiveDelete(deploymentInfoInjectedValue.getValue().getTempDir());
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
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

    public Deployment getDeployment(){
        return deploymentManager.getDeployment();
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
         * Start the web context synchronously.
         * <p/>
         * This would happen when the OSGi webapp gets explicitly started.
         */
        @Override
        public synchronized boolean startContext() {
            try {
                UndertowDeploymentService service = controller.getValue();
                service.startContext();
            } catch (Exception ex) {
                throw UndertowLogger.ROOT_LOGGER.cannotActivateContext(ex, controller.getName());
            }
            return true;
        }

        /**
         * Stop the web context synchronously.
         * <p/>
         * This would happen when the OSGi webapp gets explicitly stops.
         */
        @Override
        public synchronized boolean stopContext() {
            UndertowDeploymentService service = controller.getValue();
            service.stopContext();
            return true;
        }

        @Override
        public ServletContext getServletContext() {
            UndertowDeploymentService service = controller.getValue();
            DeploymentManager manager = service.deploymentManager;
            Deployment deployment = manager != null ? manager.getDeployment() : null;
            return deployment != null ? deployment.getServletContext() : null;
        }
    }

    private static void recursiveDelete(File file) {
        if(file == null) {
            return;
        }
        File[] files = file.listFiles();
        if (files != null){
            for(File f : files) {
                recursiveDelete(f);
            }
        }
        if(!file.delete()) {
            UndertowLogger.ROOT_LOGGER.couldNotDeleteTempFile(file);
        }
    }
}
