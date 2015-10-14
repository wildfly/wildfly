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

package org.jboss.as.ejb3.deployment.processors.security;

import javax.security.jacc.PolicyConfiguration;

import org.jboss.as.ejb3.deployment.EjbSecurityDeployer;
import org.jboss.as.security.deployment.AbstractSecurityDeployer;
import org.jboss.as.security.deployment.SecurityAttachments;
import org.jboss.as.security.service.JaccService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * A {@code DeploymentUnitProcessor} for JACC policies.
 *
 * @author Marcus Moyses
 * @author Anil Saldhana
 */
public class JaccEjbDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        boolean securityEnabled = deploymentUnit.hasAttachment(SecurityAttachments.SECURITY_ENABLED);
        if(!securityEnabled) {
            return;
        }
        AbstractSecurityDeployer<?> deployer = null;
        deployer = new EjbSecurityDeployer();
        JaccService<?> service = deployer.deploy(deploymentUnit);
        if (service != null) {
            final DeploymentUnit parentDU = deploymentUnit.getParent();
            // EJBs maybe included directly in war deployment
            ServiceName jaccServiceName = getJaccServiceName(deploymentUnit);
            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
            ServiceBuilder<?> builder = serviceTarget.addService(jaccServiceName, service);
            if (parentDU != null) {
                // add dependency to parent policy
                builder.addDependency(parentDU.getServiceName().append(JaccService.SERVICE_NAME), PolicyConfiguration.class,
                        service.getParentPolicyInjector());
            }
            builder.setInitialMode(Mode.ACTIVE).install();
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        AbstractSecurityDeployer<?> deployer = null;
        deployer = new EjbSecurityDeployer();
        deployer.undeploy(deploymentUnit);

        // EJBs maybe included directly in war deployment
        ServiceName jaccServiceName = getJaccServiceName(deploymentUnit);
        ServiceRegistry registry = deploymentUnit.getServiceRegistry();
        if(registry != null){
            ServiceController<?> serviceController = registry.getService(jaccServiceName);
            if (serviceController != null) {
                serviceController.setMode(ServiceController.Mode.REMOVE);
            }
        }
    }

    private ServiceName getJaccServiceName(DeploymentUnit deploymentUnit){
        final DeploymentUnit parentDU = deploymentUnit.getParent();
        // EJBs maybe included directly in war deployment
        ServiceName jaccServiceName = deploymentUnit.getServiceName().append(JaccService.SERVICE_NAME).append("ejb");
        //Qualify the service name properly with parent DU
        if(parentDU != null) {
            jaccServiceName = jaccServiceName.append(parentDU.getName());
        }
        return jaccServiceName;
    }
}