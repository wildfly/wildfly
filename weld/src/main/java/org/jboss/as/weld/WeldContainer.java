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

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.as.weld.deployment.WeldDeployment;
import org.jboss.as.weld.services.ModuleGroupSingletonProvider;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

/**
 * Provides access to a running weld deployment.
 * <p/>
 * Thread Safety: This class is thread safe, and requires a happens before action between construction and usage
 *
 * @author Stuart Douglas
 */
public class WeldContainer {

    private final WeldBootstrap bootstrap;
    private final WeldDeployment deployment;
    private final Environment environment;
    private final Map<String, BeanDeploymentArchive> beanDeploymentArchives;
    private volatile boolean started;

    public WeldContainer(WeldDeployment deployment, Environment environment) {
        this.deployment = deployment;
        this.environment = environment;
        this.bootstrap = new WeldBootstrap();
        Map<String, BeanDeploymentArchive> bdas = new HashMap<String, BeanDeploymentArchive>();
        for (BeanDeploymentArchive archive : deployment.getBeanDeploymentArchives()) {
            bdas.put(archive.getId(), archive);
        }
        bdas.put(deployment.getAdditionalBeanDeploymentArchive().getId(), deployment.getAdditionalBeanDeploymentArchive());
        this.beanDeploymentArchives = Collections.unmodifiableMap(bdas);
    }

    /**
     * Starts the weld container
     *
     * @throws IllegalStateException if the container is already running
     */
    public synchronized void start() {
        if (started) {
            throw WeldMessages.MESSAGES.alreadyRunning("WeldContainer");
        }
        ModuleGroupSingletonProvider.addClassLoaders(deployment.getModule().getClassLoader(), deployment.getSubDeploymentClassLoaders());
        started = true;
        ClassLoader oldTccl = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(deployment.getModule().getClassLoader());
            bootstrap.startContainer(environment, deployment);
            bootstrap.startInitialization();
            bootstrap.deployBeans();
            bootstrap.validateBeans();
            bootstrap.endInitialization();
        } finally {
            SecurityActions.setContextClassLoader(oldTccl);
        }

    }

    /**
     * Stops the container
     *
     * @throws IllegalStateException if the container is not running
     */
    public synchronized void stop() {
        if (!started) {
            throw WeldMessages.MESSAGES.notStarted("WeldContainer");
        }
        ClassLoader oldTccl = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(deployment.getModule().getClassLoader());
            bootstrap.shutdown();
        } finally {
            SecurityActions.setContextClassLoader(oldTccl);
            ModuleGroupSingletonProvider.removeClassLoader(deployment.getModule().getClassLoader());
        }
        started = false;
    }

    /**
     * Gets the {@link BeanManager} for a given bean deployment archive.
     *
     * @throws IllegalStateException if the container is not running
     */
    public BeanManager getBeanManager(BeanDeploymentArchive archive) {
        if (!started) {
            throw WeldMessages.MESSAGES.notStarted("WeldContainer");
        }
        return bootstrap.getManager(archive);
    }

    /**
     * Gets the {@link BeanManager} for a given bean deployment archive id.
     *
     * @throws IllegalStateException    if the container is not running
     * @throws IllegalArgumentException if the bean deployment archive id is not found
     */
    public BeanManager getBeanManager(String beanArchiveId) {
        if (!started) {
            throw WeldMessages.MESSAGES.notStarted("WeldContainer");
        }
        BeanDeploymentArchive beanDeploymentArchive = beanDeploymentArchives.get(beanArchiveId);
        if (beanDeploymentArchive == null) {
            throw WeldMessages.MESSAGES.beanDeploymentNotFound(beanArchiveId);
        }
        return bootstrap.getManager(beanDeploymentArchive);
    }

    /**
     * Adds a {@link Service} to the deployment. This method must not be called after the container has started
     */
    public <T extends Service> void addWeldService(Class<T> type, T service) {
        if (started) {
            throw WeldMessages.MESSAGES.cannotAddServicesAfterStart();
        }
        deployment.getServices().add(type, service);
        deployment.getAdditionalBeanDeploymentArchive().getServices().add(type, service);
    }

    /**
     * Gets the {@link BeanManager} linked to the additional classes bean deployment archive. This BeanManager has access to all
     * beans in a deployment
     *
     * @throws IllegalStateException if the container is not running
     */
    public BeanManager getBeanManager() {
        if (!started) {
            throw WeldMessages.MESSAGES.notStarted("WeldContainer");
        }
        return bootstrap.getManager(deployment.getAdditionalBeanDeploymentArchive());
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

}
