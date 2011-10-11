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

package org.jboss.as.ee.component.deployers;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.server.deployment.Attachments.SUB_DEPLOYMENTS;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.ee.component.ClassConfigurator;
import org.jboss.as.ee.component.EEModuleClassConfiguration;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.LazyValue;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassIndex;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;

/**
 * Deployment processor responsible for creating class configuration data for the whole deployment. It only runs for
 * top level deployments
 *
 * @author John Bailey
 * @author Stuart Douglas
 */
public class EEClassConfigurationProcessor implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(EEClassConfigurationProcessor.class);

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        //top level processor only, all the classes should be configured at the same time
        //as sub deployments may need to access each others classes
        if (deploymentUnit.getParent() != null) {
            return;
        }

        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            return;
        }

        final List<DeploymentUnit> subdeployments = deploymentUnit.getAttachmentList(SUB_DEPLOYMENTS);
        for (DeploymentUnit subdeployment : subdeployments) {
            processClasses(phaseContext, subdeployment);
        }
        processClasses(phaseContext, deploymentUnit);


    }

    private void processClasses(final DeploymentPhaseContext phaseContext, final DeploymentUnit subdeployment) throws DeploymentUnitProcessingException {
        final EEModuleDescription eeModuleDescription = subdeployment.getAttachment(EE_MODULE_DESCRIPTION);
        final DeploymentClassIndex classIndex = subdeployment.getAttachment(org.jboss.as.server.deployment.Attachments.CLASS_INDEX);
        if (eeModuleDescription == null) {
            // Not an EE deployment.
            return;
        }
        final Module module = subdeployment.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final Collection<EEModuleClassDescription> classDescriptions = eeModuleDescription.getClassDescriptions();
        final Map<String, LazyValue<EEModuleClassConfiguration>> results = new HashMap<String, LazyValue<EEModuleClassConfiguration>>();
        if (classDescriptions != null) {
            for (final EEModuleClassDescription classDescription : classDescriptions) {

                final LazyValue<EEModuleClassConfiguration> future = handleClassDescription(phaseContext, classDescription, classIndex);
                results.put(classDescription.getClassName(), future);
            }
        }
        eeModuleDescription.addConfiguredClasses(results);
    }

    private LazyValue<EEModuleClassConfiguration> handleClassDescription(final DeploymentPhaseContext phaseContext, final EEModuleClassDescription classDescription, final DeploymentClassIndex deploymentClassIndex) {
        //EEModuleClass's are computed in a lazy manner, as there is no guarantee that they will actually be
        //needed by a component.
        LazyValue<EEModuleClassConfiguration> future = new LazyValue<EEModuleClassConfiguration>() {

            @Override
            protected EEModuleClassConfiguration compute() {
                //as the module configurations are lazily generated if no component uses the class
                //then the exception will not be thrown
                if (classDescription.isInvalid()) {
                    throw new RuntimeException("Could not get class configuration for " + classDescription.getClassName() + " due to the following errors: " + classDescription.getInvalidMessage());
                }
                try {
                    ClassIndex index = deploymentClassIndex.classIndex(classDescription.getClassName());
                    final EEModuleClassConfiguration classConfiguration = new EEModuleClassConfiguration(index.getModuleClass(), classDescription);
                    logger.debug("Configuring EE module class: " + index.getModuleClass());
                    for (ClassConfigurator classConfigurator : classDescription.getConfigurators()) {
                        try {
                            classConfigurator.configure(phaseContext, classDescription, classConfiguration);
                        } catch (DeploymentUnitProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return classConfiguration;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Failed to load class " + classDescription.getClassName(), e);
                }


            }
        };
        return future;
    }

    public void undeploy(DeploymentUnit context) {
    }

}
