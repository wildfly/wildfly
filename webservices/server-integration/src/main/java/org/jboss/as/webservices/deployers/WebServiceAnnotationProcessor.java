/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.webservices.deployers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.metadata.property.PropertyReplacers;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public class WebServiceAnnotationProcessor implements DeploymentUnitProcessor {
    @SuppressWarnings("rawtypes")
    final List<ClassAnnotationInformationFactory> factories;

    public WebServiceAnnotationProcessor() {
        @SuppressWarnings("rawtypes")
        List<ClassAnnotationInformationFactory> factories = new ArrayList<ClassAnnotationInformationFactory>();
        factories.add(new WebServiceAnnotationInformationFactory());
        factories.add(new WebServiceProviderAnnotationInformationFactory());
        factories.add(new WebContextAnnotationInformationFactory());
        this.factories = Collections.unmodifiableList(factories);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null || eeModuleDescription == null) {
            return;
        }

        for (final ClassAnnotationInformationFactory factory : factories) {
            final Map<String, ClassAnnotationInformation<?, ?>> data = factory.createAnnotationInformation(index, PropertyReplacers.noop());
            for (Map.Entry<String, ClassAnnotationInformation<?, ?>> entry : data.entrySet()) {
                EEModuleClassDescription clazz = eeModuleDescription.addOrGetLocalClassDescription(entry.getKey());
                clazz.addAnnotationInformation(entry.getValue());
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}