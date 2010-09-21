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

import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;

import java.util.Set;
import java.util.TreeSet;

/**
 * Deployment chain implementation used to execute multiple DeploymentUnitProcessor instances in priority order.
 *
 * @author John E. Bailey
 */
public class DeploymentChainImpl implements DeploymentChain {
    private final Set<OrderedProcessor> orderedProcessors = new TreeSet<OrderedProcessor>();
    private final String name;

    public DeploymentChainImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Process the deployment unit using the chain of DeploymentUnitProcessor instances.
     *
     * @param context the deployment unit context
     * @throws org.jboss.as.deployment.unit.DeploymentUnitProcessingException
     *          if an error occurs during processing
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final Set<OrderedProcessor> processors = this.orderedProcessors;
        for(OrderedProcessor orderedProcessor : processors) {
            orderedProcessor.processor.processDeployment(context);
        }
    }

    @Override
    public void addProcessor(DeploymentUnitProcessor processor, long priority) {
        final Set<OrderedProcessor> processors = this.orderedProcessors;
        processors.add(new OrderedProcessor(processor, priority));
    }

    @Override
    public void removeProcessor(DeploymentUnitProcessor processor, long priority) {
        orderedProcessors.remove(new OrderedProcessor(processor, priority));
    }

    @Override
    public String toString() {
        return "DeploymentChainImpl{name='" + name + "' processors=" + orderedProcessors + "}";
    }

    private static class OrderedProcessor implements Comparable<OrderedProcessor> {
        private final DeploymentUnitProcessor processor;
        private final long processingOrder;

        private OrderedProcessor(final DeploymentUnitProcessor processor, final long processingOrder) {
            this.processor = processor;
            this.processingOrder = processingOrder;
        }

        @Override
        public int compareTo(final OrderedProcessor other) {
            long thisOrder = this.processingOrder;
            long otherOrder = other.processingOrder;
            return (thisOrder < otherOrder ? -1 : (thisOrder == otherOrder ? 0 : 1));
        }

        @Override
        public boolean equals(final Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            final OrderedProcessor that = (OrderedProcessor) o;
            return processingOrder == that.processingOrder && (processor != null ? processor.equals(that.processor) : that.processor == null);
        }

        @Override
        public int hashCode() {
            int result = processor != null ? processor.hashCode() : 0;
            result = 31 * result + (int) (processingOrder ^ (processingOrder >>> 32));
            return result;
        }

        public String toString() {
            return processingOrder + " => " + processor;
        }
    }
}
