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

package org.jboss.as.ee.container.injection;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import javax.annotation.Resources;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

/**
 * Configuration object used to store information for an @Resource injection for a managed bean.
 *
 * @author John E. Bailey
 */
public class ResourceInjectionConfiguration implements Serializable {
    private static final long serialVersionUID = 3405348115132260519L;

    private static final DotName RESOURCE_ANNOTATION_NAME = DotName.createSimple(Resource.class.getName());
    private static final DotName RESOURCES_ANNOTATION_NAME = DotName.createSimple(Resources.class.getName());


    /**
     * The target of the resource injection annotation.
     */
    public static enum TargetType {
        FIELD, METHOD, CLASS
    }

    private final String name;
    private final AccessibleObject target;
    private final TargetType targetType;
    private final Class<?> injectedType;
    private final String localContextName;
    private final String targetContextName;


    /**
     * Construct an instance.
     *
     * @param name              The name of the target
     * @param target            The injection target(field or method)
     * @param targetType        The type of target (field or method)
     * @param injectedType      The type of object to be injected
     * @param localContextName  The name to use in the local context
     * @param targetContextName The name to retrieve the value form
     */
    public ResourceInjectionConfiguration(final String name, final AccessibleObject target, final TargetType targetType, final Class<?> injectedType, final String localContextName, final String targetContextName) {
        this.name = name;
        this.target = target;
        this.targetType = targetType;
        this.injectedType = injectedType;
        this.localContextName = localContextName;
        this.targetContextName = targetContextName;
    }


    /**
     * Construct an instance.
     *
     * @param name              The name of the target
     * @param targetType        The type of target (field or method)
     * @param localContextName  The name to use in the local context
     * @param targetContextName The name to retrieve the value form
     */
    public ResourceInjectionConfiguration(final String name, final TargetType targetType, final String localContextName, final String targetContextName) {
        this.name = name;
        this.target = null;
        this.targetType = targetType;
        this.injectedType = null;
        this.localContextName = localContextName;
        this.targetContextName = targetContextName;
    }

    /**
     * Get the resource injection name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the injected type.
     *
     * @return The type
     */
    public Class<?> getInjectedType() {
        return injectedType;
    }

    /**
     * Get annotation type target.
     *
     * @return The target type
     */
    public TargetType getTargetType() {
        return targetType;
    }

    /**
     * Get the local context name.
     *
     * @return The local context name
     */
    public String getLocalContextName() {
        return localContextName;
    }

    /**
     * Get the target context name
     *
     * @return The target context name
     */
    public String getTargetContextName() {
        return targetContextName;
    }

    /**
     * Get the injection targer
     *
     * @return The injection target
     */
    public AccessibleObject getTarget() {
        return target;
    }

    public static List<ResourceInjectionConfiguration> from(final ClassInfo classInfo, final Class<?> beanClass, final ClassLoader beanClassLoader) {
        final List<ResourceInjectionConfiguration> configurations = new ArrayList<ResourceInjectionConfiguration>();
        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if(classAnnotations != null) {
            final List<AnnotationInstance> resourceAnnotations = classAnnotations.get(RESOURCE_ANNOTATION_NAME);
            if(resourceAnnotations != null) for (AnnotationInstance annotation : resourceAnnotations) {
                configurations.add(from(annotation, beanClass, beanClassLoader));
            }
            final List<AnnotationInstance> resourcesAnnotations = classAnnotations.get(RESOURCES_ANNOTATION_NAME);
            if(resourcesAnnotations != null && !resourcesAnnotations.isEmpty()) {
                configurations.addAll(processClassResources(beanClass));
            }
        }
        return configurations;
    }

    public static ResourceInjectionConfiguration from(final AnnotationInstance annotation, final Class<?> beanClass, final ClassLoader beanClassLoader) {
        final AnnotationTarget annotationTarget = annotation.target();
        final ResourceInjectionConfiguration resourceConfiguration;
        if (annotationTarget instanceof FieldInfo) {
            resourceConfiguration = processFieldResource(FieldInfo.class.cast(annotationTarget), beanClass);
        } else if (annotationTarget instanceof MethodInfo) {
            resourceConfiguration = processMethodResource(MethodInfo.class.cast(annotationTarget), beanClass, beanClassLoader);
        } else if (annotationTarget instanceof ClassInfo) {
            final Resource resource = beanClass.getAnnotation(Resource.class);
            if (resource == null) {
                throw new IllegalArgumentException("Failed to get @Resource annotation from class " + beanClass.getName());
            }
            resourceConfiguration = processClassResource(beanClass, resource);
        } else {
            resourceConfiguration = null;
        }
        return resourceConfiguration;
    }

    private static ResourceInjectionConfiguration processFieldResource(final FieldInfo fieldInfo, final Class<?> beanClass) {
        final String fieldName = fieldInfo.name();
        final Field field;
        try {
            field = beanClass.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Failed to get field '" + fieldName + "' from class '" + beanClass + "'", e);
        }
        final Resource resource = field.getAnnotation(Resource.class);
        if (resource != null) {
            final String localContextName = resource.name().isEmpty() ? fieldName : resource.name();
            final Class<?> injectionType = resource.type().equals(Object.class) ? field.getType() : resource.type();
            return new ResourceInjectionConfiguration(fieldName, field, ResourceInjectionConfiguration.TargetType.FIELD, injectionType, localContextName, getTargetContextName(resource, fieldName, injectionType));
        }
        return null;
    }

    private static ResourceInjectionConfiguration processMethodResource(final MethodInfo methodInfo, final Class<?> owningClass, ClassLoader beanClassLoader) {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new IllegalArgumentException("@Resource injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }
        final Class<?> argClass;
        try {
            // TODO: should I rely on DotName.toString() or compose the FQN myself?
            // TODO: to easily support primitives and arrays org.jboss.util.Classes.loadClass(String name, ClassLoader cl) could be used
            argClass = beanClassLoader.loadClass(methodInfo.args()[0].name().toString());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Failed to load " + owningClass.getName() + "." + methodName + "'s argument type " + methodInfo.args()[0].name(), e);
        }
        final Method method;
        try {
            method = owningClass.getMethod(methodName, argClass);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Failed to get method '" + methodName + "' from class '" + owningClass + "'", e);
        }
        final Resource resource = method.getAnnotation(Resource.class);
        if (resource != null) {
            final String contextNameSuffix = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            final Class<?> injectionType = resource.type().equals(Object.class) ? argClass : resource.type();
            final String localContextName = resource.name().isEmpty() ? contextNameSuffix : resource.name();
            return new ResourceInjectionConfiguration(methodName, method, ResourceInjectionConfiguration.TargetType.METHOD, injectionType, localContextName, getTargetContextName(resource, contextNameSuffix, injectionType));
        }
        return null;
    }

    private static ResourceInjectionConfiguration processClassResource(final Class<?> owningClass, final Resource resource) {
        if (resource.name().isEmpty()) {
            throw new IllegalArgumentException("Class level @Resource annotations must provide a name.");
        }
        if (resource.mappedName().isEmpty()) {
            throw new IllegalArgumentException("Class level @Resource annotations must provide a mapped name.");
        }
        if (Object.class.equals(resource.type())) {
            throw new IllegalArgumentException("Class level @Resource annotations must provide a type.");
        }
        return new ResourceInjectionConfiguration(owningClass.getName(), null, ResourceInjectionConfiguration.TargetType.CLASS, resource.type(), resource.name(), resource.mappedName());
    }

    private static List<ResourceInjectionConfiguration> processClassResources(final Class<?> owningClass) {
        final Resources resources = owningClass.getAnnotation(Resources.class);
        if(resources == null) {
            return Collections.emptyList();
        }
        final Resource[] resourceAnnotations = resources.value();
        final List<ResourceInjectionConfiguration> resourceConfigurations = new ArrayList<ResourceInjectionConfiguration>(resourceAnnotations.length);
        for(Resource resource : resourceAnnotations) {
            resourceConfigurations.add(processClassResource(owningClass, resource));
        }
        return resourceConfigurations;
    }

    private static String getTargetContextName(final Resource resource, final String contextNameSuffix, final Class<?> injectionType) {
        String targetContextName = resource.mappedName(); // TODO: Figure out how to use .lookup in IDE/Maven

        if (targetContextName.isEmpty()) {
            if (isEnvironmentEntryType(injectionType)) {
                targetContextName = contextNameSuffix;
            } else if (injectionType.isAnnotationPresent(ManagedBean.class)) {
                final ManagedBean managedBean = injectionType.getAnnotation(ManagedBean.class);
                targetContextName = managedBean.value().isEmpty() ? injectionType.getName() : managedBean.value();
            } else {
                throw new IllegalArgumentException("Unable to determine mapped name for @Resource injection.");
            }
        }
        return targetContextName;
    }

    private static boolean isEnvironmentEntryType(Class<?> type) {
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
