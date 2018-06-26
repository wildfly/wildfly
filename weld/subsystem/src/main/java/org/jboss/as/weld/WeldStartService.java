/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.services.ModuleGroupSingletonProvider;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.Container;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Service that actually finishes starting the weld container, after it has been bootstrapped by
 * {@link WeldBootstrapService}
 *
 * @author Stuart Douglas
 * @see WeldStartCompletionService
 */
public class WeldStartService implements Service<WeldStartService> {

    public static final ServiceName SERVICE_NAME = ServiceNames.WELD_START_SERVICE_NAME;

    private final InjectedValue<WeldBootstrapService> bootstrap = new InjectedValue<WeldBootstrapService>();

    private final List<SetupAction> setupActions;
    private final ClassLoader classLoader;
    private final ServiceName deploymentServiceName;

    private final AtomicBoolean runOnce = new AtomicBoolean();

    public WeldStartService(final List<SetupAction> setupActions, final ClassLoader classLoader, final ServiceName deploymentServiceName) {
        this.setupActions = setupActions;
        this.classLoader = classLoader;
        this.deploymentServiceName = deploymentServiceName;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        /*
         * Weld service restarts are not supported. Therefore, if we detect that Weld is being restarted we
         * trigger restart of the entire deployment.
         */
        if (runOnce.get()) {
            ServiceController<?> controller = context.getController().getServiceContainer().getService(deploymentServiceName);
            controller.addListener(new LifecycleListener() {
                @Override
                public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                    if (event == LifecycleEvent.DOWN) {
                        controller.setMode(Mode.ACTIVE);
                        controller.removeListener(this);
                    }
                }
            });
            controller.setMode(Mode.NEVER);
            return;
        }
        runOnce.set(true);

        ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            for (SetupAction action : setupActions) {
                action.setup(null);
            }
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            bootstrap.getValue().getBootstrap().startInitialization();
            bootstrap.getValue().getBootstrap().deployBeans();
            bootstrap.getValue().getBootstrap().validateBeans();
        } finally {

            for (SetupAction action : setupActions) {
                try {
                    action.teardown(null);
                } catch (Exception e) {
                    WeldLogger.DEPLOYMENT_LOGGER.exceptionClearingThreadState(e);
                }
            }
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }
    }

    /**
     * Stops the container
     * Executed in WeldStartService to shutdown the runtime before NamingService is closed.
     *
     * @throws IllegalStateException if the container is not running
     */
    @Override
    public void stop(final StopContext context) {
        final WeldBootstrapService bootstrapService = bootstrap.getValue();
        if (!bootstrapService.isStarted()) {
            throw WeldLogger.ROOT_LOGGER.notStarted("WeldContainer");
        }
        WeldLogger.DEPLOYMENT_LOGGER.stoppingWeldService(bootstrapService.getDeploymentName());
        ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(bootstrapService.getDeployment().getModule().getClassLoader());
            WeldProvider.containerShutDown(Container.instance(bootstrapService.getDeploymentName()));
            bootstrapService.getBootstrap().shutdown();
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
            ModuleGroupSingletonProvider.removeClassLoader(bootstrapService.getDeployment().getModule().getClassLoader());
        }
        bootstrapService.setStarted(false);
    }

    @Override
    public WeldStartService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<WeldBootstrapService> getBootstrap() {
        return bootstrap;
    }
}
