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

package org.jboss.as.deployment.managedbean.processors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

import org.jboss.as.deployment.Attachments;
import org.jboss.as.deployment.managedbean.config.InterceptorConfiguration;
import org.jboss.as.deployment.managedbean.config.ManagedBeanConfiguration;
import org.jboss.as.deployment.managedbean.config.ManagedBeanConfigurations;
import org.jboss.as.deployment.managedbean.config.ResourceConfiguration;
import org.jboss.as.server.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentPhaseContext;
import org.jboss.as.deployment.unit.DeploymentUnit;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.modules.Module;

/**
 * Deployment unit processor responsible for scanning a deployment to find classes with {@code javax.annotation.ManagedBean} annotations.
 * Note:  This processor only supports JSR-316 compliant managed beans.  So it will not handle complimentary spec additions (ex. EJB).
 *
 * @author John E. Bailey
 */
public class ManagedBeanAnnotationProcessor implements DeploymentUnitProcessor {

    private static final DotName MANAGED_BEAN_ANNOTATION_NAME = DotName.createSimple(ManagedBean.class.getName());
    private static final DotName RESOURCE_ANNOTATION_NAME = DotName.createSimple(Resource.class.getName());
    private static final DotName INTERCEPTORS_ANNOTATION_NAME = DotName.createSimple(Interceptors.class.getName());


    /**
     * Check the deployment annotation index for all classes with the @ManagedBean annotation.  For each class with the
     * annotation, collect all the required information to create a managed bean instance, and attach it to the context.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if (phaseContext.getAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY) != null) {
            return; // Skip if the configurations already exist
        }

        final Index index = phaseContext.getAttachment(Attachments.ANNOTATION_INDEX);
        if (index == null)
            return; // Skip if there is no annotation index

        final List<AnnotationInstance> instances = index.getAnnotations(MANAGED_BEAN_ANNOTATION_NAME);
        if (instances == null)
            return; // Skip if there are no ManagedBean instances

        final Module module = phaseContext.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);
        if (module == null)
            return; // Skip if there are no Module

        final ClassLoader classLoader = module.getClassLoader();

        final ManagedBeanConfigurations managedBeanConfigurations = new ManagedBeanConfigurations();
        phaseContext.putAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY, managedBeanConfigurations);

        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            if (!(target instanceof ClassInfo)) {
                throw new DeploymentUnitProcessingException("The ManagedBean annotation is only allowed at the class level: " + target);
            }
            final ClassInfo classInfo = ClassInfo.class.cast(target);
            final String beanClassName = classInfo.name().toString();
            final Class<?> beanClass;
            try {
                beanClass = classLoader.loadClass(beanClassName);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Failed to load managed bean class: " + beanClassName);
            }


            // Get the managed bean name from the annotation
            final ManagedBean managedBeanAnnotation = beanClass.getAnnotation(ManagedBean.class);
            final String beanName = managedBeanAnnotation.value().isEmpty() ? beanClassName : managedBeanAnnotation.value();
            if(managedBeanConfigurations.containsName(beanName)) {
               ManagedBeanConfiguration first = managedBeanConfigurations.getConfigurations().get(beanName);
               throw new DeploymentUnitProcessingException("Duplicate managed bean name '" + beanName + "': " + beanClassName + ", " + first.getType().getName());
            }
            final ManagedBeanConfiguration managedBeanConfiguration = new ManagedBeanConfiguration(beanName, beanClass);

            processLifecycleMethods(managedBeanConfiguration, beanClass, index);

            final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
            managedBeanConfiguration.setResourceConfigurations(processResources(classAnnotations, beanClass, classLoader));

            managedBeanConfiguration.setInterceptorConfigurations(processInterceptors(index, classAnnotations, beanClass, classLoader));

            managedBeanConfigurations.add(managedBeanConfiguration);
        }
    }

    public void undeploy(DeploymentUnit context) {
    }

    private void processLifecycleMethods(final ManagedBeanConfiguration managedBeanConfiguration,  final Class<?> beanClass, final Index index) throws DeploymentUnitProcessingException {
        final List<Method> postConstructMethods = new ArrayList<Method>();
        final List<Method> preDestroyMethods = new ArrayList<Method>();

        Class<?> current = beanClass;
        while(current != null && !Object.class.equals(current)) {
            final ClassInfo classInfo = index.getClassByName(DotName.createSimple(current.getName()));
            final Method postConstructMethod = getSingleAnnotatedMethod(current, classInfo, PostConstruct.class, false);
            if(postConstructMethod != null) {
                postConstructMethods.add(postConstructMethod);
            }
            final Method preDestroyMethod = getSingleAnnotatedMethod(current, classInfo, PreDestroy.class, false);
            if(preDestroyMethod != null) {
                preDestroyMethods.add(preDestroyMethod);
            }
            current = current.getSuperclass();
        }
        managedBeanConfiguration.setPostConstructMethods(postConstructMethods);
        managedBeanConfiguration.setPreDestroyMethods(preDestroyMethods);
    }

    private List<InterceptorConfiguration> processInterceptors(final Index index, final Map<DotName, List<AnnotationInstance>> classAnnotations, final Class<?> beanClass, ClassLoader moduleClassLoader) throws DeploymentUnitProcessingException {
        final List<AnnotationInstance> interceptorAnnotations = classAnnotations.get(INTERCEPTORS_ANNOTATION_NAME);
        if (interceptorAnnotations == null || interceptorAnnotations.isEmpty()) {
            return Collections.emptyList();
        }
        final List<InterceptorConfiguration> interceptorConfigurations = new ArrayList<InterceptorConfiguration>(interceptorAnnotations.size());

        final Interceptors interceptorsAnnotation = beanClass.getAnnotation(Interceptors.class);
        final Class<?>[] interceptorTypes = interceptorsAnnotation.value();
        for(Class<?> interceptorType : interceptorTypes) {
            final ClassInfo classInfo = index.getClassByName(DotName.createSimple(interceptorType.getName()));
            if(classInfo == null)
                continue; // TODO: Process without index info
            final Map<DotName, List<AnnotationInstance>> interceptorClassAnnotations = classInfo.annotations();

            final Method aroundInvokeMethod = getSingleAnnotatedMethod(interceptorType, classInfo, AroundInvoke.class, true);
            final List<ResourceConfiguration> resourceConfigurations = processResources(interceptorClassAnnotations, interceptorType, moduleClassLoader);
            interceptorConfigurations.add(new InterceptorConfiguration(interceptorType, aroundInvokeMethod, resourceConfigurations));
        }

        //Look for any @AroundInvoke methods on bean class
        final ClassInfo classInfo = index.getClassByName(DotName.createSimple(beanClass.getName()));
        if (classInfo != null) {
            final Method aroundInvokeMethod = getSingleAnnotatedMethod(beanClass, classInfo, AroundInvoke.class, true);
            if (aroundInvokeMethod != null) {
                final List<ResourceConfiguration> resources = processClassResources(beanClass);
                interceptorConfigurations.add(new InterceptorConfiguration(beanClass, aroundInvokeMethod, resources));
            }
        }

        return interceptorConfigurations;
    }

    private Method getSingleAnnotatedMethod(final Class<?> type, final ClassInfo classInfo, final Class<? extends Annotation> annotationType, final boolean requireInvocationContext) throws DeploymentUnitProcessingException {
        Method method = null;
        if(classInfo != null) {
            // Try to resolve with the help of the annotation index
            final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();

            final List<AnnotationInstance> instances = classAnnotations.get(DotName.createSimple(annotationType.getName()));
            if (instances == null || instances.isEmpty()) {
                return null;
            }

            if (instances.size() > 1) {
                throw new DeploymentUnitProcessingException("Only one method may be annotated with " + annotationType + " per managed bean.");
            }

            final AnnotationTarget target = instances.get(0).target();
            if (!(target instanceof MethodInfo)) {
                throw new DeploymentUnitProcessingException(annotationType + " is only valid on method targets.");
            }

            final MethodInfo methodInfo = MethodInfo.class.cast(target);
            final Type[] args = methodInfo.args();
            try {
                switch (args.length) {
                    case 0:
                        if(requireInvocationContext) {
                            throw new DeploymentUnitProcessingException("Missing argument.  Methods annotated with " + annotationType + " must have either single InvocationContext argument.");
                        }
                        method = type.getDeclaredMethod(methodInfo.name());
                        break;
                    case 1:
                        if(!InvocationContext.class.getName().equals(args[0].name().toString())) {
                            throw new DeploymentUnitProcessingException("Invalid argument type.  Methods annotated with " + annotationType + " must have either single InvocationContext argument.");
                        }
                        method = type.getDeclaredMethod(methodInfo.name(), InvocationContext.class);
                        break;
                    default:
                        throw new DeploymentUnitProcessingException("Invalid number of arguments for method " + methodInfo.name() + " annotated with " + annotationType + " on class " + type.getName());
                }
            } catch(NoSuchMethodException e) {
                throw new DeploymentUnitProcessingException("Failed to get " + annotationType + " method for type: " + type.getName(), e);
            }
        } else {
            // No index information.  Default to normal reflection
            for(Method typeMethod : type.getDeclaredMethods()) {
                if(typeMethod.isAnnotationPresent(annotationType)) {
                    method = typeMethod;
                    switch (method.getParameterTypes().length){
                        case 0:
                            if(requireInvocationContext) {
                                throw new DeploymentUnitProcessingException("Method " + method.getName() + " annotated with " + annotationType + " must have a single InvocationContext parameter");
                            }
                            break;
                        case 1:
                            if(!InvocationContext.class.equals(method.getParameterTypes()[0])) {
                                throw new DeploymentUnitProcessingException("Method " + method.getName() + " annotated with " + annotationType + " must have a single InvocationContext parameter.");
                            }
                        default:
                            throw new DeploymentUnitProcessingException("Methods " + method.getName() + " annotated with " + annotationType + " can only have a single InvocationContext parameter.");
                    }
                    break;
                }
            }
        }
        if(method != null) {
            method.setAccessible(true);
        }
        return method;
    }

    private List<ResourceConfiguration> processResources(final Map<DotName, List<AnnotationInstance>> classAnnotations, final Class<?> owningClass, ClassLoader moduleClassLoader) throws DeploymentUnitProcessingException {
        final List<AnnotationInstance> resourceAnnotations = classAnnotations.get(RESOURCE_ANNOTATION_NAME);
        if (resourceAnnotations == null) {
            return Collections.emptyList();
        }
        final List<ResourceConfiguration> resourceConfigurations = new ArrayList<ResourceConfiguration>(resourceAnnotations.size());
        for (AnnotationInstance annotation : resourceAnnotations) {
            final AnnotationTarget annotationTarget = annotation.target();
            final ResourceConfiguration resourceConfiguration;
            if (annotationTarget instanceof FieldInfo) {
                resourceConfiguration = processFieldResource(FieldInfo.class.cast(annotationTarget), owningClass);
            } else if(annotationTarget instanceof MethodInfo) {
                resourceConfiguration = processMethodResource(MethodInfo.class.cast(annotationTarget), owningClass, moduleClassLoader);
            } else if(annotationTarget instanceof ClassInfo) {
                final Resource resource = owningClass.getAnnotation(Resource.class);
                if(resource == null) {
                    throw new DeploymentUnitProcessingException("Failed to get @Resource annotation from class " + owningClass.getName());
                }
                resourceConfiguration = processClassResource(owningClass, resource);
            } else {
                continue;
            }
            if (resourceConfiguration != null) {
                resourceConfigurations.add(resourceConfiguration);
            }
        }
        resourceConfigurations.addAll(processClassResources(owningClass));
        return resourceConfigurations;
    }

    private ResourceConfiguration processFieldResource(final FieldInfo fieldInfo, final Class<?> owningClass) throws DeploymentUnitProcessingException {
        final String fieldName = fieldInfo.name();
        final Field field;
        try {
            field = owningClass.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch(NoSuchFieldException e) {
            throw new DeploymentUnitProcessingException("Failed to get field '" + fieldName + "' from class '" + owningClass + "'", e);
        }
        final Resource resource = field.getAnnotation(Resource.class);
        if (resource != null) {
            final String localContextName = resource.name().isEmpty() ? fieldName : resource.name();
            final Class<?> injectionType = resource.type().equals(Object.class) ? field.getType() : resource.type();
            return new ResourceConfiguration(fieldName, field, ResourceConfiguration.TargetType.FIELD, injectionType, localContextName, getTargetContextName(resource, fieldName, injectionType));
        }
        return null;
    }

    private ResourceConfiguration processMethodResource(final MethodInfo methodInfo, final Class<?> owningClass, ClassLoader moduleClassLoader) throws DeploymentUnitProcessingException {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new DeploymentUnitProcessingException("@Resource injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }
        final Class<?> argClass;
        try {
           // TODO: should I rely on DotName.toString() or compose the FQN myself?
           // TODO: to easily support primitives and arrays org.jboss.util.Classes.loadClass(String name, ClassLoader cl) could be used
           argClass = moduleClassLoader.loadClass(methodInfo.args()[0].name().toString());
        } catch(ClassNotFoundException e) {
           throw new DeploymentUnitProcessingException("Failed to load " + owningClass.getName() + "." + methodName + "'s argument type " + methodInfo.args()[0].name(), e);
        }
        final Method method;
        try {
            method = owningClass.getMethod(methodName, argClass);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new DeploymentUnitProcessingException("Failed to get method '" + methodName + "' from class '" + owningClass + "'", e);
        }
        final Resource resource = method.getAnnotation(Resource.class);
        if (resource != null) {
            final String contextNameSuffix = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            final Class<?> injectionType = resource.type().equals(Object.class) ? argClass : resource.type();
            final String localContextName = resource.name().isEmpty() ? contextNameSuffix : resource.name();
            return new ResourceConfiguration(methodName, method, ResourceConfiguration.TargetType.METHOD, injectionType, localContextName, getTargetContextName(resource, contextNameSuffix, injectionType));
        }
        return null;
    }

    private ResourceConfiguration processClassResource(final Class<?> owningClass, final Resource resource) throws DeploymentUnitProcessingException {
        if(resource.name().isEmpty()) {
            throw new DeploymentUnitProcessingException("Class level @Resource annotations must provide a name.");
        }
        if(resource.mappedName().isEmpty()) {
            throw new DeploymentUnitProcessingException("Class level @Resource annotations must provide a mapped name.");
        }
        if(Object.class.equals(resource.type())) {
            throw new DeploymentUnitProcessingException("Class level @Resource annotations must provide a type.");
        }
        return new ResourceConfiguration(owningClass.getName(), null, ResourceConfiguration.TargetType.CLASS, resource.type(), resource.name(), resource.mappedName());
    }

    private List<ResourceConfiguration> processClassResources(final Class<?> owningClass) throws DeploymentUnitProcessingException {
        final Resources resources = owningClass.getAnnotation(Resources.class);
        if(resources == null) {
            return Collections.emptyList();
        }
        final Resource[] resourceAnnotations = resources.value();
        final List<ResourceConfiguration> resourceConfigurations = new ArrayList<ResourceConfiguration>(resourceAnnotations.length);
        for(Resource resource : resourceAnnotations) {
            resourceConfigurations.add(processClassResource(owningClass, resource));
        }
        return resourceConfigurations;
    }

    private String getTargetContextName(final Resource resource, final String contextNameSuffix, final Class<?> injectionType) throws DeploymentUnitProcessingException {
        String targetContextName = resource.mappedName(); // TODO: Figure out how to use .lookup in IDE/Maven

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
