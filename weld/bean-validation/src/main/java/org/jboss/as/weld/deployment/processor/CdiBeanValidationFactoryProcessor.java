/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.weld.deployment.processor;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.as.ee.beanvalidation.BeanValidationAttachments;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.CdiValidatorFactoryService;
import org.jboss.as.weld.ServiceNames;
import org.jboss.msc.inject.CastingInjector;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Deployment processor that replaces the delegate of LazyValidatorFactory with a CDI-enabled ValidatorFactory.
 *
 * @author Farah Juma
 * @author Martin Kouba
 */
public class CdiBeanValidationFactoryProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final ServiceName weldStartService = topLevelDeployment.getServiceName().append(ServiceNames.WELD_START_SERVICE_NAME);
        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }
        if (!deploymentUnit.hasAttachment(BeanValidationAttachments.VALIDATOR_FACTORY)) {
            return;
        }
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        final ServiceName serviceName = deploymentUnit.getServiceName().append(CdiValidatorFactoryService.SERVICE_NAME);
        final CdiValidatorFactoryService cdiValidatorFactoryService = new CdiValidatorFactoryService(deploymentUnit);
        serviceTarget.addService(serviceName, cdiValidatorFactoryService)
            .addDependency(ServiceNames.beanManagerServiceName(deploymentUnit),
                new CastingInjector<BeanManager>(cdiValidatorFactoryService.getBeanManagerInjector(), BeanManager.class))
            .addDependency(weldStartService)
            .install();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
