/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.modcluster;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * A {@link DeploymentUnitProcessor} that adds a service dependency on mod_cluster service in order to cleanly
 * orchestrate clean shutdown. Otherwise deployments may stop concurrently with mod_cluster service possibly leading
 * to a race condition resulting in sessions not being drained.
 *
 * @author Radoslav Husar
 * @version Oct 2014
 * @see <a href="https://issues.jboss.org/browse/MODCLUSTER-399">MODCLUSTER-399</a>
 * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1083563">BZ-1083563</a>
 */
public class ModClusterServiceWebDependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // Add mod_cluster service (jboss.mod-cluster) as a web deployment dependency.
        phaseContext.getDeploymentUnit().addToAttachmentList(Attachments.WEB_DEPENDENCIES, ModClusterService.NAME);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Do nothing.
    }
}
