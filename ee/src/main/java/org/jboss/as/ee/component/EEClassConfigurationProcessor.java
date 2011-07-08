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

package org.jboss.as.ee.component;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.server.deployment.Attachments.SUB_DEPLOYMENTS;

/**
 * Deployment processor responsible for creating class configuration data for the whole deployment. It only runs for
 * top level deployments
 *
 * @author John Bailey
 */
public class EEClassConfigurationProcessor implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(EEClassConfigurationProcessor.class);

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_DESCRIPTION);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (moduleDescription == null) {
            return;
        }
        if (module == null) {
            return;
        }

        DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);

        if (deploymentUnit.getAttachment(org.jboss.as.ee.structure.Attachments.DEPLOYMENT_TYPE) == DeploymentType.EAR) {
            /*
             * We are an EAR, so we must inspect all of our subdeployments and aggregate all their component views
             * into a single index, so that inter-module resolution will work.
             */
            // Add the application description
            final Set<String> processed = new HashSet<String>();
            final List<DeploymentUnit> subdeployments = deploymentUnit.getAttachmentList(SUB_DEPLOYMENTS);
            for (DeploymentUnit subdeployment : subdeployments) {
                processClasses(phaseContext, applicationDescription,  deploymentReflectionIndex, subdeployment, processed);
            }
            processClasses(phaseContext, applicationDescription, deploymentReflectionIndex, deploymentUnit, processed);
        } else if (deploymentUnit.getParent() == null) {
            /*
             * We are a top-level EE deployment, or a non-EE deployment.  Our "aggregate" index is just a copy of
             * our local EE module index.
             */
            if (moduleDescription == null) {
                // Not an EE deployment.
                return;
            }
            final Collection<EEModuleClassDescription> classDescriptions = applicationClasses.getClassDescriptions();
            if (classDescriptions != null) {
                for (EEModuleClassDescription classDescription : classDescriptions) {
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(classDescription.getClassName(), false, module.getClassLoader());
                    } catch (ClassNotFoundException e) {
                        throw new DeploymentUnitProcessingException("Failed to load class " + classDescription.getClassName(), e);
                    }
                    final EEModuleClassConfiguration classConfiguration = new EEModuleClassConfiguration(clazz, classDescription, deploymentReflectionIndex);
                    logger.debug("Configuring EE module class: " + clazz);
                    for (ClassConfigurator classConfigurator : classDescription.getConfigurators()) {
                        classConfigurator.configure(phaseContext, classDescription, classConfiguration);
                    }
                    applicationDescription.addClass(classConfiguration);
                }
            }
        }

    }

    private void processClasses(final DeploymentPhaseContext phaseContext, final EEApplicationDescription applicationDescription, final DeploymentReflectionIndex deploymentReflectionIndex, final DeploymentUnit subdeployment, final Set<String> processed) throws DeploymentUnitProcessingException {
        final EEModuleDescription subModuleDescription = subdeployment.getAttachment(EE_MODULE_DESCRIPTION);
        if (subModuleDescription == null) {
            // Not an EE deployment.
            return;
        }
        Module subModule = subdeployment.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final EEApplicationClasses applicationClasses = subdeployment.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final Collection<EEModuleClassDescription> classDescriptions = applicationClasses.getClassDescriptions();
        if (classDescriptions != null) {
            for (EEModuleClassDescription classDescription : classDescriptions) {
                if(processed.contains(classDescription.getClassName())) {
                    continue;
                }
                processed.add(classDescription.getClassName());
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(classDescription.getClassName(), false, subModule.getClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Failed to load class " + classDescription.getClassName(), e);
                }
                final EEModuleClassConfiguration classConfiguration = new EEModuleClassConfiguration(clazz, classDescription, deploymentReflectionIndex);
                logger.debug("Configuring EE module class: " + clazz);
                for (ClassConfigurator classConfigurator : classDescription.getConfigurators()) {
                    classConfigurator.configure(phaseContext, classDescription, classConfiguration);
                }
                applicationDescription.addClass(classConfiguration);
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
