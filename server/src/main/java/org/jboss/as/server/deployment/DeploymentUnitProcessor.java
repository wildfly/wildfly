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

package org.jboss.as.server.deployment;

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

    /**
     * Perform a single step in processing the deployment phase.  The resulting state after executing this method
     * should be that either the method completes normally and all changes are made, or an exception is thrown
     * and all changes made in this method are reverted such that the original pre-invocation state is restored.
     * <p>
     * Data stored on the phase context only exists until the end of the phase.  The deployment unit context
     * which is persistent is available via {@code context.getDeploymentUnitContext()}.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException if an error occurs during processing
     */
    void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException;

    /**
     * Undo the deployment processing.  This method should undo any action taken by {@code deploy()}; however, if
     * the {@code deploy()} method added services, they need not be removed here (they will automatically be removed).
     * <p>This method should avoid throwing exceptions; any exceptions thrown are logged and ignored.  Implementations of this
     * method cannot assume that the deployment process has (or has not) proceeded beyond the current processor, nor can they
     * assume that the {@code undeploy()} method will be called from the same thread as the {@code deploy()} method.
     *
     * @param context the deployment unit context
     */
    void undeploy(DeploymentUnit context);
}
