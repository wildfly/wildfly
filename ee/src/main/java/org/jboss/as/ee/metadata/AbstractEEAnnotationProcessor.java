/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ee.metadata;

import java.util.List;
import java.util.Map;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.EJBAnnotationPropertyReplacement;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Superclass for EE annotation processors that attach their information to the EEClassDescription via {@link ClassAnnotationInformation}
 *
 * @author Stuart Douglas
 */
public abstract class AbstractEEAnnotationProcessor implements DeploymentUnitProcessor {

    public final void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        PropertyReplacer propertyReplacer = EJBAnnotationPropertyReplacement.propertyReplacer(deploymentUnit);
        if (index == null || eeModuleDescription == null) {
            return;
        }

        final List<ClassAnnotationInformationFactory> factories = annotationInformationFactories();
        for (final ClassAnnotationInformationFactory factory : factories) {
            final Map<String, ClassAnnotationInformation<?, ?>> data = factory.createAnnotationInformation(index, propertyReplacer);
            for (Map.Entry<String, ClassAnnotationInformation<?, ?>> entry : data.entrySet()) {
                EEModuleClassDescription clazz = eeModuleDescription.addOrGetLocalClassDescription(entry.getKey());
                clazz.addAnnotationInformation(entry.getValue());
            }
        }

        afterAnnotationsProcessed(phaseContext, deploymentUnit);
    }

    /**
     * Method that can be overridden to do any additional processing
     * @param phaseContext The phase context
     * @param deploymentUnit The deployment unit
     */
    protected void afterAnnotationsProcessed(final DeploymentPhaseContext phaseContext, final DeploymentUnit deploymentUnit) {
    }

    /**
     *
     * @return The annotation information factories
     */
    protected abstract List<ClassAnnotationInformationFactory> annotationInformationFactories();


    public void undeploy(final DeploymentUnit context) {

    }
}
