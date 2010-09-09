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

import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;

/**
 * Deployment chain used to execute multiple ordered DeploymentUnitProcessor instances.
 *
 * @author John E. Bailey
 */
public interface DeploymentChain extends DeploymentUnitProcessor {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("deployment", "chain");
    /**
     * Get the name of the deployment chain.  Ex. "deployment.chain.war"
     *
     * @return the name
     */
    String getName();

    /**
     * Add a new DeploymentUnitProcessor to the chain with a specified priority.
     *
     * @param processor The processor to add
     * @param priority The priority of this processor in the chain
     */
    void addProcessor(DeploymentUnitProcessor processor, long priority);

    /**
     * Remove a DeploymentUnitProcessor from the chain at a specific priority.
     * The priority is required for removal to not restrict processor instance to
     * a single location int the chain.
     *
     * @param processor The processor to removed
     * @param priority The priority location to remove the processor from
     */
    void removeProcessor(DeploymentUnitProcessor processor, long priority);
}
