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

package org.jboss.as.security.deployment;

import javax.security.jacc.PolicyConfiguration;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.security.service.JaccService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * A {@code DeploymentUnitProcessor} for JACC policies.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class JaccEarDeploymentProcessor implements DeploymentUnitProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        AbstractSecurityDeployer<?> deployer = null;
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            deployer = new EarSecurityDeployer();
            JaccService<?> service = deployer.deploy(deploymentUnit);
            if (service != null) {
                final ServiceName jaccServiceName = deploymentUnit.getServiceName().append(JaccService.SERVICE_NAME);
                final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
                ServiceBuilder<?> builder = serviceTarget.addService(jaccServiceName, service);
                if (deploymentUnit.getParent() != null) {
                    // add dependency to parent policy
                    final DeploymentUnit parentDU = deploymentUnit.getParent();
                    builder.addDependency(parentDU.getServiceName().append(JaccService.SERVICE_NAME), PolicyConfiguration.class,
                            service.getParentPolicyInjector());
                }
                builder.setInitialMode(Mode.ACTIVE).install();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undeploy(DeploymentUnit context) {
        AbstractSecurityDeployer<?> deployer = null;
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, context)) {
            deployer = new EarSecurityDeployer();
            deployer.undeploy(context);
        }
    }

}
