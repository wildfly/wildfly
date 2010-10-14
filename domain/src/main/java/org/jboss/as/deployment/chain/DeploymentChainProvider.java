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

package org.jboss.as.deployment.chain;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

/**
 * Provides deployment chain for a virtual file root.
 *
 * @author John E. Bailey
 */
public class DeploymentChainProvider implements Service<DeploymentChainProvider> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("deployment", "chain", "provider");

    public static final DeploymentChainProvider INSTANCE = new DeploymentChainProvider();

    private final Set<OrderedChainSelector> selectors = new TreeSet<OrderedChainSelector>();
    private final AtomicLong currentPriority = new AtomicLong(9999L);

    private DeploymentChainProvider() {}

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public DeploymentChainProvider getValue() throws IllegalStateException {
        return this;
    }

    /**
     * Determine which deployment chain to use for this deployment virtual file root.
     *
     * @param deploymentUnitContext The deployment context to check
     * @return The DeploymentChain responsible for this deployment root
     */
    public DeploymentChain determineDeploymentChain(final DeploymentUnitContext deploymentUnitContext) {
        for(OrderedChainSelector chainSelector : selectors) {
            if(chainSelector.deploymentChainSelector.supports(deploymentUnitContext)) {
                return chainSelector.deploymentChain;
            }
        }
        return null;
    }

    /**
     * Add a deployment chain and a selector with no specified priority.
     *
     * @param deploymentChain The deployment chain
     * @param selector The selector
     */
    public void addDeploymentChain(final DeploymentChain deploymentChain, final Selector selector) {
        selectors.add(new OrderedChainSelector(deploymentChain, selector, currentPriority.getAndIncrement()));
    }


    /**
     * Add a deployment chain and a selector.
     *
     * @param deploymentChain The deployment chain
     * @param selector The selector
     * @param priority The priority to add it
     */
    public void addDeploymentChain(final DeploymentChain deploymentChain, final Selector selector, final long priority) {
        selectors.add(new OrderedChainSelector(deploymentChain, selector, priority));
    }

    /**
     * Remove a deployment chain and a selector.
     *
     * @param deploymentChain The deployment chain
     * @param selector The selector
     * @param priority The priority to add it
     */
    public void removeDeploymentChain(final DeploymentChain deploymentChain, final Selector selector, final long priority) {
        selectors.remove(new OrderedChainSelector(deploymentChain, selector, priority));
    }

    public static class SelectorInjector<T extends Selector> implements Injector<DeploymentChainProvider> {
        private final Value<DeploymentChain> deploymentChainValue;
        private final Value<T> deploymentChainSelectorValue;
        private final long priority;
        private DeploymentChainProvider provider;

        public SelectorInjector(Value<DeploymentChain> deploymentChainValue, Value<T> deploymentChainSelectorValue, long priority) {
            this.deploymentChainValue = deploymentChainValue;
            this.deploymentChainSelectorValue = deploymentChainSelectorValue;
            this.priority = priority;
        }

        @Override
        public void inject(DeploymentChainProvider provider) throws InjectionException {
            this.provider = provider;
            provider.addDeploymentChain(deploymentChainValue.getValue(), deploymentChainSelectorValue.getValue(), priority);
        }

        @Override
        public void uninject() {
            provider.removeDeploymentChain(deploymentChainValue.getValue(), deploymentChainSelectorValue.getValue(), priority);
        }
    }

    public static interface Selector {
        boolean supports(DeploymentUnitContext deploymentUnitContext);
    }

    private static class OrderedChainSelector implements Comparable<OrderedChainSelector> {
        private final DeploymentChain deploymentChain;
        private final Selector deploymentChainSelector;
        private final long priority;

        private OrderedChainSelector(DeploymentChain deploymentChain, Selector deploymentChainSelector, long priority) {
            this.deploymentChain = deploymentChain;
            this.deploymentChainSelector = deploymentChainSelector;
            this.priority = priority;
        }

        @Override
        public int compareTo(OrderedChainSelector other) {
            long thisOrder = this.priority;
            long otherOrder = other.priority;
            return (thisOrder < otherOrder ? -1 : (thisOrder == otherOrder ? 0 : 1));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OrderedChainSelector that = (OrderedChainSelector) o;

            if (priority != that.priority) return false;
            if (deploymentChain != null ? !deploymentChain.equals(that.deploymentChain) : that.deploymentChain != null)
                return false;
            if (deploymentChainSelector != null ? !deploymentChainSelector.equals(that.deploymentChainSelector) : that.deploymentChainSelector != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = deploymentChain != null ? deploymentChain.hashCode() : 0;
            result = 31 * result + (deploymentChainSelector != null ? deploymentChainSelector.hashCode() : 0);
            result = 31 * result + (int) (priority ^ (priority >>> 32));
            return result;
        }
    }
}
