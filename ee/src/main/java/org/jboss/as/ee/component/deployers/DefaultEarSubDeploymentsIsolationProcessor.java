/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecification;

/**
 * {@link DeploymentUnitProcessor} responsible for setting the default ear subdeployments isolation for each .ear
 * deployment unit. The default is picked up from the EE subsystem and set on the {@link ModuleSpecification} of the deployment unit.
 * Unless, the specific deployment unit overrides the isolation via jboss-deployment-structure.xml, this
 * default value will be used to setup isolation of the subdeployments within a .ear.
 * <p/>
 * <b>Note: This deployer must run before the {@link org.jboss.as.server.deployment.module.descriptor.DeploymentStructureDescriptorParser}</b>
 *
 * @see {@link org.jboss.as.server.deployment.module.descriptor.DeploymentStructureDescriptorParser}
 * <p/>
 * User: Jaikiran Pai
 */
public class DefaultEarSubDeploymentsIsolationProcessor implements DeploymentUnitProcessor {

    private volatile boolean earSubDeploymentsIsolated;

    public DefaultEarSubDeploymentsIsolationProcessor() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // we only process .ear
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        // set the default ear subdeployment isolation value
        moduleSpecification.setSubDeploymentModulesIsolated(earSubDeploymentsIsolated);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    public void setEarSubDeploymentsIsolated(boolean earSubDeploymentsIsolated) {
        this.earSubDeploymentsIsolated = earSubDeploymentsIsolated;
    }


}
