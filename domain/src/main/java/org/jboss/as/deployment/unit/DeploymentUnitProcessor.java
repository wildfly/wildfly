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

package org.jboss.as.deployment.unit;

import org.jboss.msc.service.ServiceName;

/**
 * A deployment processor.  Instances of this interface represent a step in the deployer chain.  They may perform
 * a variety of tasks, including (but not limited to):
 * <ol>
 * <li>Parsing a deployment descriptor and adding it to the context</li>
 * <li>Reading a deployment descriptor's data and using it to produce deployment items</li>
 * <li>Replacing a deployment descriptor with a transformed version of that descriptor</li>
 * <li>Removing a deployment descriptor to prevent it from being processed</li>
 * </ol>
 *
 *
 */
public interface DeploymentUnitProcessor {
    static final ServiceName DEPLOYMENT_PROCESSOR_SERVICE_NAME = ServiceName.JBOSS.append("deployment", "processor");

    /**
     * Process the deployment unit.
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException if an error occurs during processing
     */
    void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException;
}
