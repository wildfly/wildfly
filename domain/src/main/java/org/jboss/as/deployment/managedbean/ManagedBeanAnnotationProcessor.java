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

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
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
import org.jboss.modules.Module;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    public static final long PRIORITY = DeploymentPhases.POST_MODULE_DESCRIPTORS.plus(100L);
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
        if (context.getAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY) != null) {
            return; // Skip if the configurations already exist
        }

        final Index index = context.getAttachment(AnnotationIndexProcessor.ATTACHMENT_KEY);
        if (index == null) {
            return; // Skip if there is no annotation index
        }

        final List<AnnotationTarget> targets = index.getAnnotationTargets(MANAGED_BEAN_ANNOTATION_NAME);
        if (targets == null) {
            return; // Skip if there are no ManagedBean instances
        }

        final Module module = context.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);
        if (module == null)
            throw new DeploymentUnitProcessingException("Manged bean annotation processing requires a module.");

        final ClassLoader classLoader = module.getClassLoader();

        final ManagedBeanConfigurations managedBeanConfigurations = new ManagedBeanConfigurations();
        context.putAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY, managedBeanConfigurations);

        for (AnnotationTarget target : targets) {
            if (!(target instanceof ClassInfo)) {
                throw new DeploymentUnitProcessingException("The ManagedBean annotation is only allowed at the class level: " + target);
            }
            final ClassInfo classInfo = ClassInfo.class.cast(target);
            final String beanClassName = classInfo.name().toString();
            final Class<?> beanCass;
            try {
                beanCass = classLoader.loadClass(beanClassName);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Failed to load managed bean class: " + beanClassName);
            }

            // Get the managed bean name from the annotation
            final ManagedBean managedBeanAnnotation = beanCass.getAnnotation(ManagedBean.class);
            final String beanName = managedBeanAnnotation.value().isEmpty() ? beanClassName : managedBeanAnnotation.value();
            final ManagedBeanConfiguration managedBeanConfiguration = new ManagedBeanConfiguration(beanName, beanCass);

            final Map<DotName, List<AnnotationTarget>> classAnnotations = classInfo.annotations();

            processLifecycleMethods(managedBeanConfiguration, classAnnotations, beanCass);

            processResources(managedBeanConfiguration, classAnnotations, beanCass);

            managedBeanConfigurations.add(managedBeanConfiguration);
        }
    }

    private void processLifecycleMethods(final ManagedBeanConfiguration managedBeanConfiguration, final Map<DotName, List<AnnotationTarget>> classAnnotations, final Class<?> beanClass) throws DeploymentUnitProcessingException {
        final String postConstructMethodName = getSingleAnnotatedNoArgMethodMethod(classAnnotations, POST_CONSTRUCT_ANNOTATION_NAME);
        if (postConstructMethodName != null) {
            try {
                final Method postConstructMethod = beanClass.getMethod(postConstructMethodName);
                managedBeanConfiguration.setPostConstructMethod(postConstructMethod);
            } catch (NoSuchMethodException e) {
                throw new DeploymentUnitProcessingException("Failed to get PostConstruct method '" + postConstructMethodName + "' for managed bean type: " + beanClass.getName(), e);
            }
        }
        final String preDestroyMethodName = getSingleAnnotatedNoArgMethodMethod(classAnnotations, PRE_DESTROY_ANNOTATION_NAME);
        if (preDestroyMethodName != null) {
            try {
                final Method preDestroyMethod = beanClass.getMethod(preDestroyMethodName);
                managedBeanConfiguration.setPreDestroyMethod(preDestroyMethod);
            } catch(NoSuchMethodException e) {
                throw new DeploymentUnitProcessingException("Failed to get PreDestroy method '" + preDestroyMethodName + "' for managed bean type: " + beanClass.getName(), e);
            }
        }
    }

    private void processResources(ManagedBeanConfiguration managedBeanConfiguration, Map<DotName, List<AnnotationTarget>> classAnnotations, final Class<?> beanClass) throws DeploymentUnitProcessingException {
        final List<AnnotationTarget> resourceInjectionTargets = classAnnotations.get(RESOURCE_ANNOTATION_NAME);
        if (resourceInjectionTargets == null) {
            managedBeanConfiguration.setResourceInjectionConfigurations(Collections.<ResourceConfiguration>emptyList());
            return;
        }
        final List<ResourceConfiguration> resourceConfigurations = new ArrayList<ResourceConfiguration>(resourceInjectionTargets.size());
        for (AnnotationTarget annotationTarget : resourceInjectionTargets) {
            final ResourceConfiguration resourceConfiguration;
            if (annotationTarget instanceof FieldInfo) {
                resourceConfiguration = processFieldResource(FieldInfo.class.cast(annotationTarget), beanClass);
            } else if(annotationTarget instanceof MethodInfo) {
                resourceConfiguration = processMethodResource(MethodInfo.class.cast(annotationTarget), beanClass);
            } else if(annotationTarget instanceof ClassInfo) {
                resourceConfiguration = processClassResource(beanClass);
            } else {
                continue;
            }
            resourceConfigurations.add(resourceConfiguration);
        }
        managedBeanConfiguration.setResourceInjectionConfigurations(resourceConfigurations);
    }

    private ResourceConfiguration processFieldResource(final FieldInfo fieldInfo, final Class<?> beanClass) throws DeploymentUnitProcessingException {
        final String fieldName = fieldInfo.name();
        final Field field;
        try {
            field = beanClass.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch(NoSuchFieldException e) {
            throw new DeploymentUnitProcessingException("Failed to get field '" + fieldName + "' from class '" + beanClass + "'", e);
        }
        final Resource resource = field.getAnnotation(Resource.class);
        final String localContextName = resource.name().isEmpty() ? fieldName : resource.name();
        final Class<?> injectionType = resource.type().equals(Object.class) ? field.getType() : resource.type();
        return new ResourceConfiguration(fieldName, field, ResourceConfiguration.TargetType.FIELD, injectionType, localContextName, getTargetContextName(resource, fieldName, injectionType));
    }

    private ResourceConfiguration processMethodResource(final MethodInfo methodInfo, final Class<?> beanClass) throws DeploymentUnitProcessingException {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new DeploymentUnitProcessingException("@Resource injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }
        final Method method;
        try {
            method = beanClass.getMethod(methodName);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new DeploymentUnitProcessingException("Failed to get method '" + methodName + "' from class '" + beanClass + "'", e);
        }
        final Resource resource = method.getAnnotation(Resource.class);
        final String contextNameSuffix = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        final Class<?> injectionType = resource.type().equals(Object.class) ? method.getReturnType() : resource.type();
        final String localContextName = resource.name().isEmpty() ? contextNameSuffix : resource.name();
        return new ResourceConfiguration(methodName, method, ResourceConfiguration.TargetType.METHOD, injectionType, localContextName, getTargetContextName(resource, contextNameSuffix, injectionType));
    }

    private ResourceConfiguration processClassResource(final Class<?> beanClass) throws DeploymentUnitProcessingException {
        final Resource resource = beanClass.getAnnotation(Resource.class);
        if(resource.name().isEmpty()) {
            throw new DeploymentUnitProcessingException("Class level @Resource annotations must provide a name.");
        }
        if(resource.mappedName().isEmpty()) {
            throw new DeploymentUnitProcessingException("Class level @Resource annotations must provide a mapped name.");
        }
        if(Object.class.equals(resource.type())) {
            throw new DeploymentUnitProcessingException("Class level @Resource annotations must provide a type.");
        }
        return new ResourceConfiguration(beanClass.getName(), null, ResourceConfiguration.TargetType.CLASS, resource.type(), resource.name(), resource.mappedName());
    }

    private String getSingleAnnotatedNoArgMethodMethod(final Map<DotName, List<AnnotationTarget>> classAnnotations, final DotName annotationName) throws DeploymentUnitProcessingException {
        final List<AnnotationTarget> targets = classAnnotations.get(annotationName);
        if (targets == null || targets.isEmpty()) {
            return null;
        }

        if (targets.size() > 1) {
            throw new DeploymentUnitProcessingException("Only one method may be annotated with " + annotationName + " per managed bean.");
        }

        final AnnotationTarget target = targets.get(0);
        if (!(target instanceof MethodInfo)) {
            throw new DeploymentUnitProcessingException(annotationName + " is only valid on method targets.");
        }

        final MethodInfo methodInfo = MethodInfo.class.cast(target);
        if (methodInfo.args().length > 0) {
            throw new DeploymentUnitProcessingException(annotationName + " methods can not have arguments");
        }
        return methodInfo.name();
    }

    private String getTargetContextName(final Resource resource, final String contextNameSuffix, final Class<?> injectionType) throws DeploymentUnitProcessingException {
        String targetContextName = resource.mappedName();
        if(targetContextName.isEmpty()) {
            if(isEnvironmentEntryType(injectionType)) {
                 targetContextName = contextNameSuffix;
            } else if(injectionType.isAnnotationPresent(ManagedBean.class)) {
                final ManagedBean managedBean = injectionType.getAnnotation(ManagedBean.class);
                 targetContextName = managedBean.value().isEmpty() ? injectionType.getName() : managedBean.value();
            } else {
                throw new DeploymentUnitProcessingException("Unable to determine mapped name for @Resource injection.");
            }
        }
        return targetContextName;
    }

    private boolean isEnvironmentEntryType(Class<?> type) {
        return type.equals(String.class)
                || type.equals(Character.class)
                || type.equals(Byte.class)
                || type.equals(Short.class)
                || type.equals(Integer.class)
                || type.equals(Long.class)
                || type.equals(Boolean.class)
                || type.equals(Double.class)
                || type.equals(Float.class)
                || type.isPrimitive();
    }
}
