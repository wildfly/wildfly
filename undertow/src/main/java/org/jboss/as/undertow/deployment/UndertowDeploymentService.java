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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ThreadSetupAction;
import io.undertow.servlet.core.CompositeThreadSetupAction;
import io.undertow.servlet.core.ContextClassLoaderSetupAction;
import org.jboss.as.clustering.ClassLoaderAwareClassResolver;
import org.jboss.as.clustering.web.DistributedCacheManagerFactory;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.undertow.Host;
import org.jboss.as.undertow.ServletContainerService;
import org.jboss.as.undertow.UndertowLogger;
import org.jboss.as.undertow.UndertowMessages;
import org.jboss.as.undertow.UndertowService;
import org.jboss.as.undertow.security.AuditNotificationReceiver;
import org.jboss.as.undertow.security.JAASIdentityManagerImpl;
import org.jboss.as.undertow.session.DistributableSessionManager;
import org.jboss.as.web.common.StartupContext;
import org.jboss.as.web.common.WebInjectionContainer;
import org.jboss.as.web.host.ContextActivator;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.audit.AuditManager;

/**
 * @author Stuart Douglas
 */
public class UndertowDeploymentService implements Service<UndertowDeploymentService> {

    private final DeploymentInfo deploymentInfo;
    private final InjectedValue<ServletContainerService> container = new InjectedValue<>();
    private final WebInjectionContainer webInjectionContainer;
    private final Module module;
    private final JBossWebMetaData jBossWebMetaData;
    private final InjectedValue<SecurityDomainContext> securityDomainContextValue = new InjectedValue<SecurityDomainContext>();
    private final InjectedValue<UndertowService> undertowService = new InjectedValue<>();
    private final InjectedValue<DistributedCacheManagerFactory> distributedCacheManagerFactoryInjectedValue = new InjectedValue<DistributedCacheManagerFactory>();
    private final InjectedValue<Host> host = new InjectedValue<>();
    private volatile DeploymentManager deploymentManager;
    private volatile DistributableSessionManager<OutgoingDistributableSessionData> sessionManager;

    public UndertowDeploymentService(final DeploymentInfo deploymentInfo, final WebInjectionContainer webInjectionContainer, final Module module, final JBossWebMetaData jBossWebMetaData) {
        this.deploymentInfo = deploymentInfo;
        this.webInjectionContainer = webInjectionContainer;
        this.module = module;
        this.jBossWebMetaData = jBossWebMetaData;

        //todo: fix this
        if(jBossWebMetaData.getDistributable() != null) {
            deploymentInfo.addOuterHandlerChainWrapper(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(final HttpHandler handler) {
                    return sessionManager.wrapHandlers(handler, deploymentManager.getDeployment());
                }
            });
        }
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        if(jBossWebMetaData.getDistributable() != null) {
            String instanceId = undertowService.getValue().getInstanceId();
            ClassResolver resolver = ModularClassResolver.getInstance(module.getModuleLoader());
            sessionManager = new DistributableSessionManager<OutgoingDistributableSessionData>(this.distributedCacheManagerFactoryInjectedValue.getValue(), jBossWebMetaData, new ClassLoaderAwareClassResolver(resolver, module.getClassLoader()), deploymentInfo.getContextPath(), module.getClassLoader(), instanceId);
            deploymentInfo.setSessionManager(sessionManager);
        }

        //TODO Darren, check this!
        final List<ThreadSetupAction> setup = new ArrayList<ThreadSetupAction>();
        setup.add(new ContextClassLoaderSetupAction(deploymentInfo.getClassLoader()));
        setup.addAll(deploymentInfo.getThreadSetupActions());
        final CompositeThreadSetupAction threadSetupAction = new CompositeThreadSetupAction(setup);

        SecurityDomainContext sdc = securityDomainContextValue.getValue();
        deploymentInfo.setIdentityManager(new JAASIdentityManagerImpl(sdc, jBossWebMetaData.getPrincipalVersusRolesMap(), threadSetupAction));
        AuditManager auditManager = sdc.getAuditManager();
        if (auditManager != null) {
            deploymentInfo.addNotificationReceiver(new AuditNotificationReceiver(auditManager));
        }
        deploymentInfo.setConfidentialPortManager(getConfidentialPortManager());
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
        deploymentInfo.setIdentityManager(null);
        sessionManager = null;
        host.getValue().unregisterDeployment(deploymentInfo);
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

    public InjectedValue<SecurityDomainContext> getSecurityDomainContextValue() {
        return securityDomainContextValue;
    }

    public InjectedValue<UndertowService> getUndertowService() {
        return undertowService;
    }

    private ConfidentialPortManager getConfidentialPortManager() {
        return new ConfidentialPortManager() {

            @Override
            public int getConfidentialPort(HttpServerExchange exchange) {
                return container.getValue().lookupSecurePort("default");
            }
        };
    }

    public InjectedValue<DistributedCacheManagerFactory> getDistributedCacheManagerFactoryInjectedValue() {
        return distributedCacheManagerFactoryInjectedValue;
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
