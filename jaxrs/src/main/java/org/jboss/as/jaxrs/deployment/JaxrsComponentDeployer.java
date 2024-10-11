/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs.deployment;

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;
import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Arrays;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.managedbean.component.ManagedBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.resteasy.util.GetRestful;
import org.jboss.as.ee.component.ConcurrencyAttachments;

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
    private static final String SESSION_BEAN_DESCRIPTION_CLASS_NAME = "org.jboss.as.ejb3.component.session.SessionBeanComponentDescription";

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

            if (isInstanceOf(component, SESSION_BEAN_DESCRIPTION_CLASS_NAME)) {
                if (isInstanceOf(component, STATEFUL_SESSION_BEAN_DESCRIPTION_CLASS_NAME)) {
                    //using SFSB's as Jakarta RESTful Web Services endpoints is not recommended, but if people really want to do it they can

                    JAXRS_LOGGER.debugf("Stateful session bean %s is being used as a Jakarta RESTful Web Services endpoint, this is not recommended", component.getComponentName());
                    if (partOfWeldDeployment) {
                        //if possible just let CDI handle the integration
                        continue;
                    }
                }

                Class<?>[] jaxrsType = GetRestful.getSubResourceClasses(componentClass);
                final String jndiName;
                if (component.getViews().size() == 1) {
                    //only 1 view, just use the simple JNDI name
                    jndiName = "java:app/" + moduleDescription.getModuleName() + "/" + component.getComponentName();
                } else {
                    boolean found = false;
                    String foundType = null;
                    for (final ViewDescription view : component.getViews()) {
                        for (Class<?> subResource : jaxrsType) {
                            if (view.getViewClassName().equals(subResource.getName())) {
                                foundType = subResource.getName();
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                    if (!found) {
                        throw JAXRS_LOGGER.typeNameNotAnEjbView(Arrays.asList(jaxrsType), component.getComponentName());
                    }
                    jndiName = "java:app/" + moduleDescription.getModuleName() + "/" + component.getComponentName() + "!" + foundType;
                }

                JAXRS_LOGGER.debugf("Found Jakarta RESTful Web Services Managed Bean: %s local jndi jaxRsTypeName: %s", component.getComponentClassName(), jndiName);
                resteasy.getScannedJndiComponentResources().add(jndiName + ";" + component.getComponentClassName() + ";" + "true");
                // make sure its removed from list
                resteasy.getScannedResourceClasses().remove(component.getComponentClassName());
            } else if (component instanceof ManagedBeanComponentDescription) {
                String jndiName = "java:app/" + moduleDescription.getModuleName() + "/" + component.getComponentName();

                JAXRS_LOGGER.debugf("Found Jakarta RESTful Web Services Managed Bean: %s local jndi name: %s", component.getComponentClassName(), jndiName);
                resteasy.getScannedJndiComponentResources().add(jndiName + ";" + component.getComponentClassName() + ";" + "true");
                // make sure it's removed from list
                resteasy.getScannedResourceClasses().remove(component.getComponentClassName());
            }
        }
    }

    private boolean isInstanceOf(ComponentDescription component, String className) {
        Class<?> c = component.getClass();
        while (c != Object.class && c != null) {
            if(c.getName().equals(className)) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }
}
