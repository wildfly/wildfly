/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    public void setEarSubDeploymentsIsolated(boolean earSubDeploymentsIsolated) {
        this.earSubDeploymentsIsolated = earSubDeploymentsIsolated;
    }


}
