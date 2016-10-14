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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.WeldDeployment;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.services.ModuleGroupSingletonProvider;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.Container;
import org.jboss.weld.ContainerState;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Provides the initial bootstrap of the Weld container. This does not actually finish starting the container, merely gets it to
 * the point that the bean manager is available.
 *
 * @author Stuart Douglas
 */
public class WeldBootstrapService implements Service<WeldBootstrapService> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("WeldBootstrapService");

    private final WeldBootstrap bootstrap;
    private final WeldDeployment deployment;
    private final Environment environment;
    private final Map<String, BeanDeploymentArchive> beanDeploymentArchives;
    private final BeanDeploymentArchiveImpl rootBeanDeploymentArchive;

    private final String deploymentName;

    private final InjectedValue<SecurityServices> securityServices = new InjectedValue<SecurityServices>();
    private final InjectedValue<TransactionServices> weldTransactionServices = new InjectedValue<TransactionServices>();
    private final InjectedValue<ExecutorServices> executorServices = new InjectedValue<ExecutorServices>();
    private final InjectedValue<ExecutorService> serverExecutor = new InjectedValue<ExecutorService>();

    private volatile boolean started;

    public WeldBootstrapService(WeldDeployment deployment, Environment environment, final String deploymentName) {
        this.deployment = deployment;
        this.environment = environment;
        this.deploymentName = deploymentName;
        this.bootstrap = new WeldBootstrap();
        Map<String, BeanDeploymentArchive> bdas = new HashMap<String, BeanDeploymentArchive>();
        BeanDeploymentArchiveImpl rootBeanDeploymentArchive = null;
        for (BeanDeploymentArchive archive : deployment.getBeanDeploymentArchives()) {
            bdas.put(archive.getId(), archive);
            if (archive instanceof BeanDeploymentArchiveImpl) {
                BeanDeploymentArchiveImpl bda = (BeanDeploymentArchiveImpl) archive;
                if (bda.isRoot()) {
                    rootBeanDeploymentArchive = bda;
                }
            }
        }
        this.rootBeanDeploymentArchive = rootBeanDeploymentArchive;
        this.beanDeploymentArchives = Collections.unmodifiableMap(bdas);
    }

    /**
     * Starts the weld container
     *
     * @throws IllegalStateException if the container is already running
     */
    public synchronized void start(final StartContext context) {
        if (started) {
            throw WeldLogger.ROOT_LOGGER.alreadyRunning("WeldContainer");
        }
        started = true;

        WeldLogger.DEPLOYMENT_LOGGER.startingWeldService(deploymentName);
        // set up injected services
        addWeldService(SecurityServices.class, securityServices.getValue());

        TransactionServices transactionServices = weldTransactionServices.getOptionalValue();
        if (transactionServices != null) {
            addWeldService(TransactionServices.class, transactionServices);
        }

        if (!deployment.getServices().contains(ExecutorServices.class)) {
            addWeldService(ExecutorServices.class, executorServices.getValue());
        }

        ModuleGroupSingletonProvider.addClassLoaders(deployment.getModule().getClassLoader(),
                deployment.getSubDeploymentClassLoaders());

        ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(deployment.getModule().getClassLoader());
            bootstrap.startContainer(deploymentName, environment, deployment);
            WeldProvider.containerInitialized(Container.instance(deploymentName), getBeanManager(), deployment);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }

    }

    /**
     * This is a no-op if {@link WeldStartService#start(StartContext)} completes normally and the shutdown is performed in
     * {@link WeldStartService#stop(org.jboss.msc.service.StopContext)}.
     */
    public synchronized void stop(final StopContext context) {
        if (started) {
            // WeldStartService#stop() not completed - attempt to perform the container cleanup
            final Container container = Container.instance(deploymentName);
            if (container != null && !ContainerState.SHUTDOWN.equals(container.getState())) {
                final ExecutorService executorService = serverExecutor.getValue();
                final Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        WeldLogger.DEPLOYMENT_LOGGER.debugf("Weld container cleanup for deployment %s", deploymentName);
                        ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                        try {
                            WildFlySecurityManager
                                    .setCurrentContextClassLoaderPrivileged(deployment.getModule().getClassLoader());
                            WeldProvider.containerShutDown(container);
                            container.setState(ContainerState.SHUTDOWN);
                            container.cleanup();
                            setStarted(false);
                        } finally {
                            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
                            ModuleGroupSingletonProvider.removeClassLoader(deployment.getModule().getClassLoader());
                            context.complete();
                        }
                    }
                };
                try {
                    executorService.execute(task);
                } catch (RejectedExecutionException e) {
                    task.run();
                } finally {
                    context.asynchronous();
                }
            }
        }
    }

    /**
     * Gets the {@link BeanManager} for a given bean deployment archive id.
     *
     * @throws IllegalStateException if the container is not running
     * @throws IllegalArgumentException if the bean deployment archive id is not found
     */
    public BeanManagerImpl getBeanManager(String beanArchiveId) {
        if (!started) {
            throw WeldLogger.ROOT_LOGGER.notStarted("WeldContainer");
        }
        BeanDeploymentArchive beanDeploymentArchive = beanDeploymentArchives.get(beanArchiveId);
        if (beanDeploymentArchive == null) {
            throw WeldLogger.ROOT_LOGGER.beanDeploymentNotFound(beanArchiveId);
        }
        return bootstrap.getManager(beanDeploymentArchive);
    }

    /**
     * Adds a {@link Service} to the deployment. This method must not be called after the container has started
     */
    public <T extends org.jboss.weld.bootstrap.api.Service> void addWeldService(Class<T> type, T service) {
        deployment.addWeldService(type, service);
    }

    /**
     * Gets the {@link BeanManager} linked to the root bean deployment archive. This BeanManager has access to all beans in a
     * deployment
     *
     * @throws IllegalStateException if the container is not running
     */
    public BeanManagerImpl getBeanManager() {
        if (!started) {
            throw WeldLogger.ROOT_LOGGER.notStarted("WeldContainer");
        }
        return bootstrap.getManager(rootBeanDeploymentArchive);
    }

    /**
     * get all beans deployment archives in the deployment
     */
    public Set<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return new HashSet<BeanDeploymentArchive>(beanDeploymentArchives.values());
    }

    public boolean isStarted() {
        return started;
    }

    void setStarted(boolean started) {
        this.started = started;
    }

    WeldDeployment getDeployment() {
        return deployment;
    }

    String getDeploymentName() {
        return deploymentName;
    }

    WeldBootstrap getBootstrap() {
        return bootstrap;
    }

    @Override
    public WeldBootstrapService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<SecurityServices> getSecurityServices() {
        return securityServices;
    }

    public InjectedValue<TransactionServices> getWeldTransactionServices() {
        return weldTransactionServices;
    }

    public InjectedValue<ExecutorServices> getExecutorServices() {
        return executorServices;
    }

    public InjectedValue<ExecutorService> getServerExecutor() {
        return serverExecutor;
    }
}
