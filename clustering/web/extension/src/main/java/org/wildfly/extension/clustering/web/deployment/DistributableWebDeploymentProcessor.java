/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web.deployment;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.clustering.web.session.DistributableSessionManagementProvider;

/**
 * {@link DeploymentUnitProcessor} that attaches any configured {@link DistributableSessionManagementProvider} to the deployment unit.
 * @author Paul Ferraro
 */
public class DistributableWebDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();
        DistributableSessionManagementProvider provider = context.getAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY);
        if (provider != null) {
            unit.putAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY, provider);
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
        unit.removeAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY);
    }
}
