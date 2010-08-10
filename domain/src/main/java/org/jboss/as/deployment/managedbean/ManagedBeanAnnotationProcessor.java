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

package org.jboss.as.deployment.managedbean;

import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Deployment unit processor responsible for scanning a deployment to find classes with {@code javax.annotation.ManagedBean} annotations.
 * Note:  This processor only supports JSR-316 compliant managed beans.  So it will not handle complimentary spec additions (ex. EJB). 
 *
 * @author John E. Bailey
 */
public class ManagedBeanAnnotationProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = AnnotationIndexProcessor.PRIORITY + 10;
    private static final DotName MANAGED_BEAN_ANNOTATION_NAME = DotName.createSimple(ManagedBean.class.getName());
    private static final DotName POST_CONSTRUCT_ANNOTATION_NAME = DotName.createSimple(PostConstruct.class.getName());
    private static final DotName PRE_DESTROY_ANNOTATION_NAME = DotName.createSimple(PreDestroy.class.getName());
    private static final DotName RESOURCE_ANNOTATION_NAME = DotName.createSimple(Resource.class.getName());

    /**
     * Check the deployment annotation index for all classes with the @ManagedBean annotation.  For each class with the
     * annotation, collect all the required information to create a managed bean instance, and attach it to the context.   
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        ManagedBeanConfigurations managedBeanConfigurations = context.getAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY);
        if(managedBeanConfigurations != null) {
            return; // Skip if the configurations already exist
        }
        final Index index = context.getAttachment(AnnotationIndexProcessor.ATTACHMENT_KEY);
        if(index == null) {
            return; // Skip if there is no annotation index
        }

        final List<AnnotationTarget> targets = index.getAnnotationTargets(MANAGED_BEAN_ANNOTATION_NAME);
        if(targets == null) {
            return; // Skip if there are no ManagedBean instances
        }

        managedBeanConfigurations = new ManagedBeanConfigurations();
        context.putAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY, managedBeanConfigurations);

        for (AnnotationTarget target : targets) {
            if (!(target instanceof ClassInfo)) {
                throw new DeploymentUnitProcessingException("The ManagedBean annotation is only allowed at the class level: " + target, null); 
            }
            final ClassInfo classInfo = ClassInfo.class.cast(target);

            final ManagedBeanConfiguration managedBeanConfiguration = new ManagedBeanConfiguration(classInfo.name().toString());

            final Map<DotName, List<AnnotationTarget>> classAnnotations = classInfo.annotations();

            processLifecycleMethods(managedBeanConfiguration, classAnnotations);

            processResourceInjections(managedBeanConfiguration, classAnnotations);

            managedBeanConfigurations.add(managedBeanConfiguration);
        }
    }

    private void processLifecycleMethods(final ManagedBeanConfiguration managedBeanConfiguration, final Map<DotName, List<AnnotationTarget>> classAnnotations) throws DeploymentUnitProcessingException {
        final String postConstructMethod = getSingleAnnotatedNoArgMethodMethod(classAnnotations, POST_CONSTRUCT_ANNOTATION_NAME);
        if (postConstructMethod != null) {
            managedBeanConfiguration.setPostConstructMethod(postConstructMethod);
        }
        final String preDestroyMethod = getSingleAnnotatedNoArgMethodMethod(classAnnotations, PRE_DESTROY_ANNOTATION_NAME);
        if (preDestroyMethod != null) {
            managedBeanConfiguration.setPreDestroyMethod(preDestroyMethod);
        }
    }

    private void processResourceInjections(ManagedBeanConfiguration managedBeanConfiguration, Map<DotName, List<AnnotationTarget>> classAnnotations) throws DeploymentUnitProcessingException {
        final List<AnnotationTarget> resourceInjectionTargets = classAnnotations.get(RESOURCE_ANNOTATION_NAME);
        if(resourceInjectionTargets == null) {
            managedBeanConfiguration.setResourceInjectionConfigurations(Collections.<ResourceInjectionConfiguration>emptyList());
        }
        final List<ResourceInjectionConfiguration> resourceInjectionConfigurations = new ArrayList<ResourceInjectionConfiguration>(resourceInjectionTargets.size());
        for(AnnotationTarget annotationTarget : resourceInjectionTargets) {
            if(annotationTarget instanceof FieldInfo) {
                final FieldInfo fieldInfo = FieldInfo.class.cast(annotationTarget);
                resourceInjectionConfigurations.add(new ResourceInjectionConfiguration(fieldInfo.name(), ResourceInjectionConfiguration.TargetType.FIELD, fieldInfo.type().name().toString()));
            } else if(annotationTarget instanceof MethodInfo) {
                final MethodInfo methodInfo = MethodInfo.class.cast(annotationTarget);
                final String methodName = methodInfo.name();
                final Type[] args = methodInfo.args();
                if(!methodName.startsWith("set") || args.length != 1) {
                    throw new DeploymentUnitProcessingException("@Resource injection target is invalid.  Only setter methods are allowed: " + methodInfo, null);
                }
                resourceInjectionConfigurations.add(new ResourceInjectionConfiguration(methodName, ResourceInjectionConfiguration.TargetType.METHOD, args[0].name().toString()));
            }
        }
    }


    private String getSingleAnnotatedNoArgMethodMethod(final Map<DotName, List<AnnotationTarget>> classAnnotations, final DotName annotationName) throws DeploymentUnitProcessingException {
        final List<AnnotationTarget> targets = classAnnotations.get(annotationName);
        if (targets == null || targets.isEmpty()) {
            return null;
        }

        if (targets.size() > 1) {
            throw new DeploymentUnitProcessingException("Only one method may be annotated with " + annotationName + " per managed bean.", null);
        }

        final AnnotationTarget target = targets.get(0);
        if (!(target instanceof MethodInfo)) {
            throw new DeploymentUnitProcessingException(annotationName + " is only valid on method targets.", null);
        }

        final MethodInfo methodInfo = MethodInfo.class.cast(target);
        if (methodInfo.args().length > 0) {
            throw new DeploymentUnitProcessingException(annotationName + " methods can not have arguments", null);
        }
        return methodInfo.name();
    }
}
