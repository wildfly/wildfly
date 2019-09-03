/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import javax.servlet.ServletException;

import org.jboss.as.web.common.StartupContext;
import org.jboss.as.web.common.WebInjectionContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UndertowDeploymentService implements Service<UndertowDeploymentService> {

    private final Consumer<UndertowDeploymentService> serviceConsumer;
    private final Supplier<ServletContainerService> container;
    private final Supplier<ExecutorService> serverExecutor;
    private final Supplier<Host> host;
    private final Supplier<DeploymentInfo> deploymentInfo;
    private final WebInjectionContainer webInjectionContainer;
    private final boolean autostart;

    private volatile DeploymentManager deploymentManager;

    UndertowDeploymentService(
            final Consumer<UndertowDeploymentService> serviceConsumer, final Supplier<ServletContainerService> container,
            final Supplier<ExecutorService> serverExecutor, final Supplier<Host> host, final Supplier<DeploymentInfo> deploymentInfo,
            final WebInjectionContainer webInjectionContainer, final boolean autostart) {
        this.serviceConsumer = serviceConsumer;
        this.container = container;
        this.serverExecutor = serverExecutor;
        this.host = host;
        this.deploymentInfo = deploymentInfo;
        this.webInjectionContainer = webInjectionContainer;
        this.autostart = autostart;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        if (autostart) {
            // The start can trigger the web app context initialization which involves blocking tasks like
            // servlet context initialization, startup servlet initialization lifecycles and such. Hence this needs to be done asynchronously
            // to prevent the MSC threads from blocking
            startContext.asynchronous();
            serverExecutor.get().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        startContext();
                        startContext.complete();
                    } catch (Throwable e) {
                        startContext.failed(new StartException(e));
                    }
                }
            });
        }
    }

    public void startContext() throws ServletException {
        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        DeploymentInfo deploymentInfo = this.deploymentInfo.get();
        Thread.currentThread().setContextClassLoader(deploymentInfo.getClassLoader());
        try {
            StartupContext.setInjectionContainer(webInjectionContainer);
            try {
                deploymentManager = container.get().getServletContainer().addDeployment(deploymentInfo);
                deploymentManager.deploy();
                HttpHandler handler = deploymentManager.start();
                Deployment deployment = deploymentManager.getDeployment();
                host.get().registerDeployment(deployment, handler);
            } finally {
                StartupContext.setInjectionContainer(null);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
            serviceConsumer.accept(this);
        }
    }

    @Override
    public void stop(final StopContext stopContext) {
        // The service stop can trigger the web app context destruction which involves blocking tasks like servlet context destruction, startup servlet
        // destruction lifecycles and such. Hence this needs to be done asynchronously to prevent the MSC threads from blocking
        stopContext.asynchronous();
        serverExecutor.get().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    stopContext();
                } finally {
                    stopContext.complete();
                }
            }
        });

    }

    public void stopContext() {
        serviceConsumer.accept(null);
        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        DeploymentInfo deploymentInfo = this.deploymentInfo.get();
        Thread.currentThread().setContextClassLoader(deploymentInfo.getClassLoader());
        try {
            if (deploymentManager != null) {
                Deployment deployment = deploymentManager.getDeployment();
                try {
                    host.get().unregisterDeployment(deployment);
                    deploymentManager.stop();
                } catch (ServletException e) {
                    throw new RuntimeException(e);
                }
                deploymentManager.undeploy();
                container.get().getServletContainer().removeDeployment(deploymentInfo);
            }
            recursiveDelete(deploymentInfo.getTempDir());
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public UndertowDeploymentService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public DeploymentInfo getDeploymentInfo() {
        return deploymentInfo.get();
    }

    public Deployment getDeployment(){
        if(this.deploymentManager != null) {
            return deploymentManager.getDeployment();
        } else {
            return null;
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
