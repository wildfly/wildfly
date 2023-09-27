/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.structure;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.EEClassIntrospector;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

import java.util.List;
import java.util.Map;

import static org.jboss.as.ee.component.Attachments.*;
import static org.jboss.as.server.deployment.Attachments.SUB_DEPLOYMENTS;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Stuart Douglas
 */
public final class ComponentAggregationProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final ComponentRegistry componentRegistry = new ComponentRegistry(phaseContext.getServiceRegistry());

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(EE_MODULE_DESCRIPTION);
        if(moduleDescription == null) {
            return;
        }

        final ServiceName sn = ComponentRegistry.serviceName(deploymentUnit);
        final ServiceBuilder<?> sb = phaseContext.getServiceTarget().addService(sn);
        sb.setInstance(new ValueService(componentRegistry));
        sb.addDependency(moduleDescription.getDefaultClassIntrospectorServiceName(), EEClassIntrospector.class, componentRegistry.getClassIntrospectorInjectedValue()).install();

        deploymentUnit.putAttachment(COMPONENT_REGISTRY, componentRegistry);

        if (deploymentUnit.getAttachment(Attachments.DEPLOYMENT_TYPE) == DeploymentType.EAR) {

            final EEApplicationDescription applicationDescription = new EEApplicationDescription();
            deploymentUnit.putAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_DESCRIPTION, applicationDescription);

            for (final Map.Entry<String, String> messageDestination : moduleDescription.getMessageDestinations().entrySet()) {
                applicationDescription.addMessageDestination(messageDestination.getKey(), messageDestination.getValue(), deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT).getRoot());
            }

            /*
            * We are an EAR, so we must inspect all of our subdeployments and aggregate all their component views
            * into a single index, so that inter-module resolution will work.
            */
            // Add the application description
            final List<DeploymentUnit> subdeployments = deploymentUnit.getAttachmentList(SUB_DEPLOYMENTS);
            for (final DeploymentUnit subdeployment : subdeployments) {
                final EEModuleDescription subDeploymentModuleDescription = subdeployment.getAttachment(EE_MODULE_DESCRIPTION);
                final ResourceRoot deploymentRoot = subdeployment.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
                if (subDeploymentModuleDescription == null) {
                    // Not an EE deployment.
                    continue;
                }
                for (final ComponentDescription componentDescription : subDeploymentModuleDescription.getComponentDescriptions()) {
                    applicationDescription.addComponent(componentDescription, deploymentRoot.getRoot());
                }
                for (final Map.Entry<String, String> messageDestination : subDeploymentModuleDescription.getMessageDestinations().entrySet()) {
                    applicationDescription.addMessageDestination(messageDestination.getKey(), messageDestination.getValue(), deploymentRoot.getRoot());
                }
                for (final ComponentDescription componentDescription : subdeployment.getAttachmentList(org.jboss.as.ee.component.Attachments.ADDITIONAL_RESOLVABLE_COMPONENTS)) {
                    applicationDescription.addComponent(componentDescription, deploymentRoot.getRoot());
                }

                subdeployment.putAttachment(EE_APPLICATION_DESCRIPTION, applicationDescription);
            }
        } else if (deploymentUnit.getParent() == null) {

            final EEApplicationDescription applicationDescription = new EEApplicationDescription();
            deploymentUnit.putAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_DESCRIPTION, applicationDescription);
            /*
             * We are a top-level EE deployment, or a non-EE deployment.  Our "aggregate" index is just a copy of
             * our local EE module index.
             */
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);

            for (final ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
                applicationDescription.addComponent(componentDescription, deploymentRoot.getRoot());
            }
            for (final Map.Entry<String, String> messageDestination : moduleDescription.getMessageDestinations().entrySet()) {
                applicationDescription.addMessageDestination(messageDestination.getKey(), messageDestination.getValue(), deploymentRoot.getRoot());
            }
            for (final ComponentDescription componentDescription : deploymentUnit.getAttachmentList(org.jboss.as.ee.component.Attachments.ADDITIONAL_RESOLVABLE_COMPONENTS)) {
                applicationDescription.addComponent(componentDescription, deploymentRoot.getRoot());
            }
        }
    }

    private static final class ValueService implements Service<ComponentRegistry> {
        private final ComponentRegistry value;
        public ValueService(final ComponentRegistry value) {
            this.value = value;
        }

        public void start(final StartContext context) {
            // noop
        }

        public void stop(final StopContext context) {
            // noop
        }

        public ComponentRegistry getValue() {
            return value;
        }
    }
}
