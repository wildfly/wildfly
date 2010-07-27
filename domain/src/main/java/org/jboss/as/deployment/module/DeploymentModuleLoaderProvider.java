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

package org.jboss.as.deployment.module;

import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.jboss.vfs.VirtualFile;

import java.util.Set;
import java.util.TreeSet;

/**
 * Provides the deployment module loader that supports a deployment.
 * 
 * @author John E. Bailey
 */
public class DeploymentModuleLoaderProvider implements Service<DeploymentModuleLoaderProvider> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("deployment", "module", "loader", "provider");

    private final Set<OrderedLoaderSelector> selectors = new TreeSet<OrderedLoaderSelector>();

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public DeploymentModuleLoaderProvider getValue() throws IllegalStateException {
        return this;
    }

    /**
     * Determine which deployment module loader to use for this deployment virtual file root.
     *
     * @param root The root to check
     * @return The DeploymentModuleLoader responsible for this deployment root
     */
    public DeploymentModuleLoader determineDeploymentModuleLoader(final VirtualFile root) {
        for(OrderedLoaderSelector loaderSelector : selectors) {
            if(loaderSelector.selector.supports(root)) {
                return loaderSelector.moduleLoader;
            }
        }
        return null;
    }

    /**
     * Add a deployment module loader and a selector.
     *
     * @param deploymentModuleLoader The deployment module loader
     * @param selector The selector
     * @param priority The priority to add it
     */
    public void addModuleLoader(final DeploymentModuleLoader deploymentModuleLoader, final Selector selector, final long priority) {
        selectors.add(new OrderedLoaderSelector(deploymentModuleLoader, selector, priority));
    }

    /**
     * Remove a deployment module loader.
     *
     * @param deploymentModuleLoader The deployment module loader
     * @param selector The selector
     * @param priority The priority to add it
     */
    public void removeModuleLoader(final DeploymentModuleLoader deploymentModuleLoader, final Selector selector, final long priority) {
        selectors.remove(new OrderedLoaderSelector(deploymentModuleLoader, selector, priority));
    }

    public static class SelectorInjector<T extends Selector> implements Injector<DeploymentModuleLoaderProvider> {
        private final Value<DeploymentModuleLoader> deploymentModuleLoaderValue;
        private final Value<T> selectorValue;
        private final long priority;
        private DeploymentModuleLoaderProvider provider;

        public SelectorInjector(Value<DeploymentModuleLoader> deploymentModuleLoaderValue, Value<T> selectorValue, long priority) {
            this.deploymentModuleLoaderValue = deploymentModuleLoaderValue;
            this.selectorValue = selectorValue;
            this.priority = priority;
        }

        @Override
        public void inject(DeploymentModuleLoaderProvider provider) throws InjectionException {
            this.provider = provider;
            provider.addModuleLoader(deploymentModuleLoaderValue.getValue(), selectorValue.getValue(), priority);
        }

        @Override
        public void uninject() {
            provider.removeModuleLoader(deploymentModuleLoaderValue.getValue(), selectorValue.getValue(), priority);
        }
    }

    public static interface Selector {
        boolean supports(VirtualFile virtualFile);
    }

    private static class OrderedLoaderSelector implements Comparable<OrderedLoaderSelector> {
        private final DeploymentModuleLoader moduleLoader;
        private final Selector selector;
        private final long priority;

        private OrderedLoaderSelector(DeploymentModuleLoader moduleLoader, Selector selector, long priority) {
            this.moduleLoader = moduleLoader;
            this.selector = selector;
            this.priority = priority;
        }

        @Override
        public int compareTo(OrderedLoaderSelector other) {
            long thisOrder = this.priority;
            long otherOrder = other.priority;
            return (thisOrder < otherOrder ? -1 : (thisOrder == otherOrder ? 0 : 1));
        }
    }
}
