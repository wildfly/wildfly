/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.managedbean.processors;

import java.util.List;

import javax.annotation.ManagedBean;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.naming.ContextServiceNameBuilder;
import org.jboss.as.managedbean.component.ManagedBeanComponentConfiguration;
import org.jboss.as.managedbean.component.ManagedBeanComponentFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.msc.service.ServiceName;

/**
 * Deployment unit processor responsible for scanning a deployment to find classes with {@code javax.annotation.ManagedBean} annotations.
 * Note:  This processor only supports JSR-316 compliant managed beans.  So it will not handle complimentary spec additions (ex. EJB).
 *
 * @author John E. Bailey
 */
public class ManagedBeanAnnotationProcessor implements DeploymentUnitProcessor {

    static final DotName MANAGED_BEAN_ANNOTATION_NAME = DotName.createSimple(ManagedBean.class.getName());

    /**
     * Check the deployment annotation index for all classes with the @ManagedBean annotation.  For each class with the
     * annotation, collect all the required information to create a managed bean instance, and attach it to the context.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if(compositeIndex == null) {
            return;
        }
        final List<AnnotationInstance> instances = compositeIndex.getAnnotations(MANAGED_BEAN_ANNOTATION_NAME);
        if (instances == null || instances.isEmpty()) {
            return;
        }

        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            if (!(target instanceof ClassInfo)) {
                throw new DeploymentUnitProcessingException("The ManagedBean annotation is only allowed at the class level: " + target);
            }
            final ClassInfo classInfo = ClassInfo.class.cast(target);
            final String beanClassName = classInfo.name().toString();

            // Get the managed bean name from the annotation
            final AnnotationValue nameValue = instance.value();
            final String beanName = nameValue == null || nameValue.asString().isEmpty() ? beanClassName : nameValue.asString();
            final ComponentConfiguration componentConfiguration = new ManagedBeanComponentConfiguration(beanName, beanClassName, ManagedBeanComponentFactory.INSTANCE);

            componentConfiguration.setAppContextServiceName(ContextServiceNameBuilder.app(deploymentUnit));

            final ServiceName moduleContextServiceName = ContextServiceNameBuilder.module(deploymentUnit);
            componentConfiguration.setModuleContextServiceName(moduleContextServiceName);
            componentConfiguration.setCompContextServiceName(moduleContextServiceName);
            componentConfiguration.setEnvContextServiceName(moduleContextServiceName.append("env"));

            deploymentUnit.addToAttachmentList(org.jboss.as.ee.component.Attachments.COMPONENT_CONFIGS, componentConfiguration);
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
