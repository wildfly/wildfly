/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class TransactionManagementAnnotationProcessor extends AbstractComponentConfigProcessor {
    private static final DotName TRANSACTION_MANAGEMENT_ANNOTATION_NAME = DotName.createSimple(TransactionManagement.class.getName());

    @Override
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final ComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final ClassInfo classInfo = index.getClassByName(DotName.createSimple(componentDescription.getComponentClassName()));
        if (classInfo == null) {
            return; // We can't continue without the annotation index info.
        }
        // Only process EJB deployments
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit) || !(componentDescription instanceof EJBComponentDescription)) {
            return;
        }
        processTransactionManagement(classInfo, index, (EJBComponentDescription) componentDescription);
    }

    protected void processTransactionManagement(final ClassInfo classInfo, final CompositeIndex index, final EJBComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        // EJB 3.1 FR 13.3.6 The TransactionManagement annotation is applied to the enterprise bean class.
        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if (classAnnotations != null) {
            List<AnnotationInstance> annotations = classAnnotations.get(TRANSACTION_MANAGEMENT_ANNOTATION_NAME);
            if (annotations != null) {
                assert annotations.size() == 1 : "@TransactionManagement can only be on the class itself";
                final AnnotationValue annotationValue = annotations.get(0).value();
                // annotation value can be null if the user specifies @TransactionManagement without any explicit value.
                // Jandex API doesn't return the "default" value and that's the expected API behaviour
                // https://issues.jboss.org/browse/AS7-1506
                final TransactionManagementType txManagementType = annotationValue == null ?
                        TransactionManagementType.CONTAINER : TransactionManagementType.valueOf(annotationValue.asEnum());
                componentDescription.setTransactionManagementType(txManagementType);
            }
        }
    }

}
