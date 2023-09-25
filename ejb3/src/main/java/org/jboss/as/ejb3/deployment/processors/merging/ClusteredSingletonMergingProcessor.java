/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.clustering.EJBBoundClusteringMetaData;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.ClusteredSingleton;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;

import java.util.List;

/**
 * Handles ClusteredSingleton merging.
 *
 * @author Flavia Rainone
 */
public class ClusteredSingletonMergingProcessor extends AbstractMergingProcessor<MessageDrivenComponentDescription> {

    public ClusteredSingletonMergingProcessor() {
        super(MessageDrivenComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(DeploymentUnit deploymentUnit, EEApplicationClasses applicationClasses,
            DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass,
            MessageDrivenComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        //we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }
        final ClassAnnotationInformation<ClusteredSingleton, Boolean> clustering = clazz.getAnnotationInformation(ClusteredSingleton.class);
        if (clustering == null || clustering.getClassLevelAnnotations().isEmpty()) {
            return;
        }
        componentDescription.setClusteredSingleton(true);
    }

    @Override
    protected void handleDeploymentDescriptor(DeploymentUnit deploymentUnit,
            DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass,
            MessageDrivenComponentDescription componentDescription) throws DeploymentUnitProcessingException {

        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        Boolean allBeansClusteredSingleton = null;
        if (ejbJarMetaData != null) {
            final AssemblyDescriptorMetaData assemblyMetadata = ejbJarMetaData.getAssemblyDescriptor();
            if (assemblyMetadata != null) {
                final List<EJBBoundClusteringMetaData> clusteringMetaDatas = assemblyMetadata.getAny(EJBBoundClusteringMetaData.class);
                if (clusteringMetaDatas != null) {
                    for (final EJBBoundClusteringMetaData clusteringMetaData : clusteringMetaDatas) {
                        if ("*".equals(clusteringMetaData.getEjbName())) {
                            allBeansClusteredSingleton = clusteringMetaData.isClusteredSingleton();
                        } else if (componentDescription.getComponentName().equals(clusteringMetaData.getEjbName())) {
                            componentDescription.setClusteredSingleton(clusteringMetaData.isClusteredSingleton());
                            return;
                        }
                    }
                }
            }
        }
        if (allBeansClusteredSingleton != null && allBeansClusteredSingleton) {
            componentDescription.setClusteredSingleton(true);
        }
    }
}
