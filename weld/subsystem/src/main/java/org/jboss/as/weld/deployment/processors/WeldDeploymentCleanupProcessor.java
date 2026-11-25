/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processors;

import static org.jboss.as.weld.deployment.AttachmentKeys.START_COMPLETION_DEPENDENCIES;

import java.util.ArrayList;
import java.util.Collections;
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
        // require misc services from the top level deployment
        for (ServiceName sn : getMiscServiceDependencies(deploymentUnit)) {
            weldStartCompletionServiceBuilder.requires(sn);
        }
        // require misc services from sub-deployments
        for (DeploymentUnit sub : subDeployments) {
            for (ServiceName sn : getMiscServiceDependencies(sub)) {
                weldStartCompletionServiceBuilder.requires(sn);
            }
        }

        weldStartCompletionServiceBuilder.setInstance(new WeldStartCompletionService(bootstrapSupplier,
                WeldDeploymentProcessor.getSetupActions(deploymentUnit), module.getClassLoader()));
        weldStartCompletionServiceBuilder.install();
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

    private List<ServiceName> getMiscServiceDependencies(DeploymentUnit deploymentUnit) {
        List<ServiceName> list = deploymentUnit.getAttachmentList(START_COMPLETION_DEPENDENCIES);
        return list == null ? Collections.emptyList() : list;
    }
}
