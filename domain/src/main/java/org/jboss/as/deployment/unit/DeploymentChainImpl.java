/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment.unit;

import java.util.Set;
import java.util.TreeSet;

/**
 * Default deployment chain used to execute multiple ordered DeploymentUnitProcessor instances.
 *
 * @author John E. Bailey
 */
public class DeploymentChainImpl implements DeploymentChain {

    private final DeploymentUnitProcessor[] processors;

    private DeploymentChainImpl(final DeploymentUnitProcessor[] processors) {
        this.processors = processors;
    }

    /**
     * Process the deployment unit using the chain of DeploymentUnitProcessor instances.
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     *          if an error occurs during processing
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final DeploymentUnitProcessor[] processors = this.processors;
        for(DeploymentUnitProcessor processor : processors) {
            processor.processDeployment(context);
        }
    }

    /**
     * Create a new DeploymentChainImpl builder.
     *
     * @return the builder
     */
    public static Builder build() {
        return new Builder() {
            private final Set<OrderedProcessor> orderedProcessors = new TreeSet<OrderedProcessor>();
            private boolean created;

            @Override
            public Builder addProcessor(DeploymentUnitProcessor processor, long processingOrder) {
                checkCreated();
                final Set<OrderedProcessor> processors = this.orderedProcessors;
                processors.add(new OrderedProcessor(processor, processingOrder));
                return this;
            }

            @Override
            public DeploymentChainImpl create() {
                checkCreated();
                created = true;
                final Set<OrderedProcessor> orderedProcessors = this.orderedProcessors;
                final DeploymentUnitProcessor[] processors = new DeploymentUnitProcessor[orderedProcessors.size()];
                int i = 0;
                for(final OrderedProcessor orderedProcessor : orderedProcessors) {
                    processors[i++] = orderedProcessor.processor;
                }
                return new DeploymentChainImpl(processors);
            }

            private void checkCreated() {
                if(created)
                    throw new IllegalStateException("DeploymentChainImpl has already been created");
            }
        };
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
    }
}
