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
package org.jboss.as.ejb3.deployment.processors.merging;

import java.util.List;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.resourceadapterbinding.metadata.EJBBoundResourceAdapterBindingMetaData;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;

/**
 * Handles the {@link org.jboss.ejb3.annotation.ResourceAdapter} annotation merging
 *
 * @author Stuart DouglasrunAs
 */
public class ResourceAdaptorMergingProcessor extends AbstractMergingProcessor<MessageDrivenComponentDescription> {

    public ResourceAdaptorMergingProcessor() {
        super(MessageDrivenComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final MessageDrivenComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {

        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());

        //we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }
        final ClassAnnotationInformation<ResourceAdapter, String> resourceAdaptor = clazz.getAnnotationInformation(ResourceAdapter.class);
        if (resourceAdaptor == null) {
            return;
        }
        if (!resourceAdaptor.getClassLevelAnnotations().isEmpty()) {
            componentConfiguration.setResourceAdapterName(resourceAdaptor.getClassLevelAnnotations().get(0));
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final MessageDrivenComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {

        final String ejbName = componentConfiguration.getEJBName();
        final EjbJarMetaData metaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (metaData == null) {
            return;
        }
        final AssemblyDescriptorMetaData assemblyDescriptor = metaData.getAssemblyDescriptor();
        if (assemblyDescriptor == null) {
            return;
        }
        final List<EJBBoundResourceAdapterBindingMetaData> resourceAdapterBindingDataList = assemblyDescriptor.getAny(EJBBoundResourceAdapterBindingMetaData.class);

        String resourceAdapterName = null;
        if (resourceAdapterBindingDataList != null) {
            for (EJBBoundResourceAdapterBindingMetaData resourceAdapterBindingData: resourceAdapterBindingDataList) {
                if ("*".equals(resourceAdapterBindingData.getEjbName()) && resourceAdapterName == null) {
                    resourceAdapterName = resourceAdapterBindingData.getResourceAdapterName();
                } else if (ejbName.equals(resourceAdapterBindingData.getEjbName())) {
                    resourceAdapterName = resourceAdapterBindingData.getResourceAdapterName();
                }
            }
        }
        if (resourceAdapterName != null) {
            componentConfiguration.setResourceAdapterName(resourceAdapterName);
        }
    }
}
