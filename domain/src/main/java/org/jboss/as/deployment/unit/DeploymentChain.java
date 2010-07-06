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

/**
 * Deployment chain used to execute multiple ordered DeploymentUnitProcessor instances.
 * 
 * @author John E. Bailey
 */
public interface DeploymentChain extends DeploymentUnitProcessor {
    /**
     * Builder used to create DeploymentChainImpl instances.
     */
    public static interface Builder {
        /**
         * Add a new DeploymentUnitProcessor.
         *
         * @param processor       The processor to add
         * @param processingOrder The expected order this processor should be in the chain
         * @return The builder instance for chaining.
         */
        Builder addProcessor(DeploymentUnitProcessor processor, long processingOrder);

        /**
         * Create the DeploymentChainImpl instance.
         *
         * @return The DeploymentChainImpl
         */
        DeploymentChainImpl create();
    }
}
