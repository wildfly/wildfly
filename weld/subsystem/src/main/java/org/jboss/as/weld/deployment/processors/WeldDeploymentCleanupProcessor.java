/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.deployment.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld._private.WeldDeploymentMarker;
import org.jboss.as.weld.WeldStartCompletionService;
import org.jboss.as.weld.WeldStartService;
import org.jboss.as.weld.util.Utils;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * A processor which takes care of after boot cleanup for Weld. The idea is to invoke
 * {@code org.jboss.weld.bootstrap.WeldStartup.endInitialization()} after all EE components are installed.
 *
 * This allows Weld to do metadata cleanup on unused items.
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WeldDeploymentCleanupProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parent = Utils.getRootDeploymentUnit(deploymentUnit);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        // obtain service names
        ServiceName weldStartCompletionServiceName = parent.getServiceName().append(WeldStartCompletionService.SERVICE_NAME);
        ServiceName weldBootstrapServiceName = parent.getServiceName().append(WeldBootstrapService.SERVICE_NAME);
        ServiceName weldStartServiceName = parent.getServiceName().append(WeldStartService.SERVICE_NAME);

        // if it is not Weld deployment, we skip it
        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }

        // only register this on top level deployments
        if (deploymentUnit.getParent() != null) {
            return;
        }

        // add dependency on our WeldBootstrapService and WeldStartService to ensure this goes after them
        ServiceBuilder<?> weldStartCompletionServiceBuilder = serviceTarget.addService(weldStartCompletionServiceName);
        final Supplier<WeldBootstrapService> bootstrapSupplier = weldStartCompletionServiceBuilder.requires(weldBootstrapServiceName);
        weldStartCompletionServiceBuilder.requires(weldStartServiceName);
        // require component start services from top level deployment
        for (ServiceName componentStartSN : getComponentStartServiceNames(deploymentUnit)) {
            weldStartCompletionServiceBuilder.requires(componentStartSN);
        }
        // require component start services from sub-deployments
        final List<DeploymentUnit> subDeployments = deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
        for (DeploymentUnit sub : subDeployments) {
            ServiceRegistry registry = sub.getServiceRegistry();
            List<ServiceName> componentStartServiceNames = getComponentStartServiceNames(sub);
            for (ServiceName componentStartSN : componentStartServiceNames) {
                weldStartCompletionServiceBuilder.requires(componentStartSN);
            }
        }

        weldStartCompletionServiceBuilder.setInstance(new WeldStartCompletionService(bootstrapSupplier,
                WeldDeploymentProcessor.getSetupActions(deploymentUnit), module.getClassLoader()));
        weldStartCompletionServiceBuilder.install();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // no-op
    }

    private List<ServiceName> getComponentStartServiceNames(DeploymentUnit deploymentUnit) {
        List<ServiceName> serviceNames = new ArrayList<>();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        if (eeModuleDescription == null) {
            return serviceNames;
        }
        for (ComponentDescription component : eeModuleDescription.getComponentDescriptions()) {
            serviceNames.add(component.getStartServiceName());
        }
        return serviceNames;
    }
}
