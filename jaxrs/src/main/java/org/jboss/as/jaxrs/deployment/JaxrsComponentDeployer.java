/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs.deployment;

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;
import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.List;
import java.util.Set;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ConcurrencyAttachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.resteasy.util.GetRestful;

/**
 * Integrates Jakarta RESTful Web Services with managed beans and Jakarta Enterprise Beans's
 *
 * @author Stuart Douglas
 */
public class JaxrsComponentDeployer implements DeploymentUnitProcessor {

    /**
     * We use hard coded class names to avoid a direct dependency on Jakarta Enterprise Beans
     * <p>
     * This allows the use of Jakarta RESTful Web Services in cut down servers without Jakarta Enterprise Beans
     * </p>
     * <p>
     * Kinda yuck, but there is not really any alternative if we want don't want the dependency
     * </p>
     */
    private static final String STATEFUL_SESSION_BEAN_DESCRIPTION_CLASS_NAME = "org.jboss.as.ejb3.component.stateful.StatefulComponentDescription";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            return;
        }


        final ResteasyDeploymentData resteasy = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);
        if (resteasy == null) {
            return;
        }

        // Set up the context for managed threads
        phaseContext.getDeploymentUnit().addToAttachmentList(ConcurrencyAttachments.ADDITIONAL_FACTORIES, ResteasyContextHandleFactory.INSTANCE);

        // right now I only support resources
        if (!resteasy.isScanResources()) return;

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }

        final ClassLoader loader = module.getClassLoader();

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
        boolean partOfWeldDeployment = false;
        if (support.hasCapability(WELD_CAPABILITY_NAME)) {
            final WeldCapability weldCapability = support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).orElse(null);
            partOfWeldDeployment = weldCapability != null && weldCapability.isPartOfWeldDeployment(deploymentUnit);
        }
        for (final ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            Class<?> componentClass;
            try {
                componentClass = loader.loadClass(component.getComponentClassName());
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException(e);
            }
            if (!GetRestful.isRootResource(componentClass)) continue;
            if (isSFSB(component)) {
                //using SFSB's as Jakarta RESTful Web Services endpoints is not recommended, but if people really want to do it they can

                JAXRS_LOGGER.debugf("Stateful session bean %s is being used as a Jakarta RESTful Web Services endpoint, this is not recommended", component.getComponentName());
                if (partOfWeldDeployment) {
                    //if possible just let CDI handle the integration
                    continue;
                }
            }
            boolean found = false;
            // Find a binding name for the resource. We simply use the first binding name we find that starts with java:app
            // for the component. We assume the component will have the correct view that we need.
            for (ViewDescription view : component.getViews()) {
                final Set<String> bindings = view.getBindingNames();
                if (!bindings.isEmpty()) {
                    found = true;
                    final String jndiName = bindings.stream()
                            .filter(name -> name.startsWith("java:app"))
                            .findFirst()
                            .orElseThrow(() -> JAXRS_LOGGER.typeNameNotAnEjbView(List.of(GetRestful.getRootResourceClass(componentClass)), component.getComponentName()));

                    JAXRS_LOGGER.debugf("Found Jakarta RESTful Web Services Managed Bean: %s local jndi jaxRsTypeName: %s", component.getComponentClassName(), jndiName);
                    // Resource naming is jndi-name;component-class;cache
                    resteasy.getScannedJndiComponentResources()
                            .add(jndiName + ";" + component.getComponentClassName() + ";true");
                    // make sure it's removed from the scanned resources list
                    resteasy.getScannedResourceClasses().remove(component.getComponentClassName());
                    break;
                }
            }
            if (!found) {
                throw JAXRS_LOGGER.typeNameNotAnEjbView(List.of(GetRestful.getRootResourceClass(componentClass)), component.getComponentName());
            }
        }
    }

    private boolean isSFSB(ComponentDescription component) {
        Class<?> c = component.getClass();
        while (c != Object.class && c != null) {
            if (c.getName().equals(STATEFUL_SESSION_BEAN_DESCRIPTION_CLASS_NAME)) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }
}
