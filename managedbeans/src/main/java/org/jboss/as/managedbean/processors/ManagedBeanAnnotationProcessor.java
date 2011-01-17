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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jboss.as.ee.container.builder.BeanContainerConfigBuilder;
import static org.jboss.as.ee.container.Util.getSingleAnnotatedMethod;
import org.jboss.as.managedbean.config.ManagedBeanConfigurations;
import org.jboss.as.managedbean.container.ManagedBeanContainerFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexUtils;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;

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
        if (deploymentUnit.getAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY) != null) {
            return; // Skip if the configurations already exist
        }

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null)
            return; // Skip if there are no Module

        Map<ResourceRoot, Index> indexes = AnnotationIndexUtils.getAnnotationIndexes(deploymentUnit);
        for (Entry<ResourceRoot, Index> entry : indexes.entrySet()) {
            final Index index = entry.getValue();

            final List<AnnotationInstance> instances = index.getAnnotations(MANAGED_BEAN_ANNOTATION_NAME);
            if (instances == null)
                continue; // Skip if there are no ManagedBean instances

            final ClassLoader classLoader = module.getClassLoader();

            final ManagedBeanConfigurations managedBeanConfigurations = new ManagedBeanConfigurations();
            deploymentUnit.putAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY, managedBeanConfigurations);

            for (AnnotationInstance instance : instances) {
                AnnotationTarget target = instance.target();
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException(
                            "The ManagedBean annotation is only allowed at the class level: " + target);
                }
                final ClassInfo classInfo = ClassInfo.class.cast(target);
                final String beanClassName = classInfo.name().toString();
                final Class<?> beanClass;
                try {
                    beanClass = classLoader.loadClass(beanClassName);
                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Failed to load managed bean class: " + beanClassName, e);
                }

                // Get the managed bean name from the annotation
                final ManagedBean managedBeanAnnotation = beanClass.getAnnotation(ManagedBean.class);
                final String beanName = managedBeanAnnotation.value().isEmpty() ? beanClassName : managedBeanAnnotation.value();

                final BeanContainerConfigBuilder builder = BeanContainerConfigBuilder.build(beanClass, classLoader, new ManagedBeanContainerFactory())
                        .setName(beanName);
                processLifecycleMethods(builder, beanClass, index);
                builder.processAnnotations(classInfo, index);

                deploymentUnit.addToAttachmentList(org.jboss.as.ee.container.service.Attachments.BEAN_CONTAINER_CONFIGS, builder.create());
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }

    private void processLifecycleMethods(final BeanContainerConfigBuilder beanContainerConfigBuilder, final Class<?> beanClass, final Index index) throws DeploymentUnitProcessingException {

        Class<?> current = beanClass;
        while (current != null && !Object.class.equals(current)) {
            final ClassInfo classInfo = index.getClassByName(DotName.createSimple(current.getName()));
            final Method postConstructMethod = getSingleAnnotatedMethod(current, classInfo, PostConstruct.class, false);
            if (postConstructMethod != null) {
                beanContainerConfigBuilder.addPostConstruct(postConstructMethod);
            }
            final Method preDestroyMethod = getSingleAnnotatedMethod(current, classInfo, PreDestroy.class, false);
            if (preDestroyMethod != null) {
                beanContainerConfigBuilder.addPreDestroy(preDestroyMethod);
            }
            current = current.getSuperclass();
        }
    }
}
