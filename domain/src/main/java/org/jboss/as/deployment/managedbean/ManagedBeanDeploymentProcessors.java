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
import org.jboss.as.deployment.naming.ContextNames;
import org.jboss.as.deployment.naming.OptionalNamingLookupValue;
import org.jboss.as.deployment.naming.ResourceBinder;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Values;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deployment unit processors responsible for adding deployment items for each managed bean configuration.
 *
 * @author John E. Bailey
 */
public class ManagedBeanDeploymentProcessors implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(200L);

    private static final Map<String, Class> PRIMITIVE_NAME_TYPE_MAP = new HashMap<String, Class>();
    static {
        PRIMITIVE_NAME_TYPE_MAP.put("boolean", Boolean.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("byte", Byte.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("char", Character.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("short", Short.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("int", Integer.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("long", Long.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("float", Float.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("double", Double.TYPE);
    }

    /**
     * Process the deployment and add a managed bean service for each managed bean configuration in the deployment.
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final ManagedBeanConfigurations managedBeanConfigurations = context.getAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY);
        if(managedBeanConfigurations == null)
            return; // Skip deployments with no managed beans.

        final Module module = context.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);
        if(module == null)
            throw new DeploymentUnitProcessingException("Manged bean deployment processing requires a module.", null);

        final BatchBuilder batchBuilder = context.getBatchBuilder();

        for(ManagedBeanConfiguration managedBeanConfiguration : managedBeanConfigurations.getConfigurations().values()) {
            processManagedBean(context.getName(), managedBeanConfiguration, batchBuilder, module.getClassLoader());
        }
    }

    private void processManagedBean(final String deploymentName, final ManagedBeanConfiguration managedBeanConfiguration, final BatchBuilder batchBuilder, final ClassLoader classLoader) throws DeploymentUnitProcessingException {
        final String beanClassName = managedBeanConfiguration.getType();
        final Class<Object> beanClass;
        try {
            beanClass = (Class<Object>) classLoader.loadClass(beanClassName);
        } catch(ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Failed to load managed bean class: " + beanClassName, null);
        }
        final ManagedBean managedBeanAnnotation = beanClass.getAnnotation(ManagedBean.class);
        if(managedBeanAnnotation == null)
            throw new DeploymentUnitProcessingException("Can not find the @MangedBean annotation for class " + beanClass, null);
        final String name = managedBeanAnnotation.value() != null ? managedBeanAnnotation.value() : beanClass.getName();

        final String postConstructMethodName = managedBeanConfiguration.getPostConstructMethod();
        Method postConstructMethod = null;
        try {
            if(postConstructMethodName != null) {
                postConstructMethod = beanClass.getMethod(postConstructMethodName);
            }
        } catch(NoSuchMethodException e) {
            throw new DeploymentUnitProcessingException("Failed to get PostConstruct method '" + postConstructMethodName + "' for managed bean type: " + beanClass.getName(), e, null);
        }

        final String preDestroyMethodName = managedBeanConfiguration.getPreDestroyMethod();
        Method preDestroyMethod = null;
        try {
            if(preDestroyMethodName != null) {
                preDestroyMethod = beanClass.getMethod(preDestroyMethodName);
            }
        } catch(NoSuchMethodException e) {
            throw new DeploymentUnitProcessingException("Failed to get PreDestroy method '" + preDestroyMethodName + "' for managed bean type: " + beanClass.getName(), e, null);
        }

        final List<ResourceInjection<?>> resourceInjections = new ArrayList<ResourceInjection<?>>();
        final ManagedBeanService<Object> managedBeanService = new ManagedBeanService<Object>(beanClass, postConstructMethod, preDestroyMethod, resourceInjections);

        final Class<?> managedBeanClass;
        try {
            managedBeanClass = classLoader.loadClass(managedBeanConfiguration.getType());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load managed bean class", e);
        }


        final ServiceName managedBeanServiceName = ManagedBeanService.SERVICE_NAME.append(deploymentName, name);
        final BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(managedBeanServiceName, managedBeanService);
        for (ResourceInjectionConfiguration resourceInjectionConfiguration : managedBeanConfiguration.getResourceInjectionConfigurations()) {
            final ResourceInjection<Object> resourceInjection = processResourceInjection(resourceInjectionConfiguration, managedBeanClass, batchBuilder, serviceBuilder, deploymentName, classLoader);
            resourceInjections.add(resourceInjection);
        }
        // TODO: Get naming context and add a ResourceBinder for this managed bean
        final Reference managedBeanFactoryReference = ManagedBeanObjectFactory.createReference(beanClass, managedBeanServiceName);
        final ResourceBinder<Reference> managedBeanFactoryBinder = new ResourceBinder<Reference>("global/" + deploymentName + "/" + name, Values.immediateValue(managedBeanFactoryReference));
        batchBuilder.addService(managedBeanServiceName.append("factorybinder"), managedBeanFactoryBinder)
            .addDependency(ContextNames.JAVA, Context.class, managedBeanFactoryBinder.getContextInjector())
            .addDependency(managedBeanServiceName);
    }

    private ResourceInjection<Object> processResourceInjection(final ResourceInjectionConfiguration resourceInjectionConfiguration, final Class<?> managedBeanClass, final BatchBuilder batchBuilder, final BatchServiceBuilder serviceBuilder, final String deploymentName, final ClassLoader classLoader) throws DeploymentUnitProcessingException {
        final String targetName = resourceInjectionConfiguration.getName();
        final String injectedType = resourceInjectionConfiguration.getInjectedType();
        final boolean primitiveTarget = isPrimative(injectedType);
        final String contextNameSuffix;
        final Resource resource;
        final ResourceInjection<Object> resourceInjection;
        // Determine where to bind the injected value

        if(ResourceInjectionConfiguration.TargetType.FIELD.equals(resourceInjectionConfiguration.getTargetType())) {
            final Field field;
            try {
                field = managedBeanClass.getDeclaredField(targetName);
            } catch(NoSuchFieldException e) {
                throw new RuntimeException("Failed to get field '" + targetName + "' from class '" + managedBeanClass + "'", e);
            }
            resource = field.getAnnotation(Resource.class);
            contextNameSuffix = field.getName();
            resourceInjection = new FieldResourceInjection<Object>(targetName, primitiveTarget);
        } else {
            final Method method;
            try {
                method = managedBeanClass.getMethod(targetName);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Failed to get method '" + targetName + "' from class '" + managedBeanClass + "'", e);
            }
            resource = method.getAnnotation(Resource.class);
            final String methodName = method.getName();
            contextNameSuffix = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            resourceInjection = new MethodResourceInjection<Object>(targetName, injectedType, primitiveTarget);
        }
        if(!resource.type().equals(Object.class)) {
            resourceInjectionConfiguration.setInjectedType(resource.type().getName());
        }
        final String contextName = !"".equals(resource.name()) ? resource.name() : managedBeanClass.getName() + "/" + contextNameSuffix;
        final ServiceName resourceBinderBaseName = ResourceBinder.MODULE_BINDER.append(deploymentName);
        final ServiceName binderName = resourceBinderBaseName.append(contextName);
        serviceBuilder.addDependency(binderName, resourceInjection.getValueInjector());

        // Determine the correct mapped name
        String mappedName = resource.mappedName();
        if(mappedName == null || "".equals(mappedName)) {
            final Class<?> type;
            try {
                type = loadClass(injectedType, classLoader);
            } catch(ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Failed to load injection target class: " + injectedType, e, null);
            }
            if(isEnvironmentEntryType(type)) {
                mappedName = contextNameSuffix;
            } else if(type.isAnnotationPresent(ManagedBean.class)) {
                final ManagedBean managedBean = type.getAnnotation(ManagedBean.class);
                mappedName = !"".equals(managedBean.value()) ? managedBean.value() : type.getName();
            } else {
                throw new DeploymentUnitProcessingException("Unable to determine mapped name for @Resource injection.", null);
            }
        }

        // Now add a binder for the local context
        final OptionalNamingLookupValue<Object> bindValue = new OptionalNamingLookupValue<Object>(mappedName);
        final ResourceBinder<Object> resourceBinder = new ResourceBinder<Object>(contextName, bindValue);
        final BatchServiceBuilder<Object> binderServiceBuilder = batchBuilder.addService(binderName, resourceBinder);
        final ServiceName moduleContextName = ContextNames.MODULE.append(deploymentName);
        binderServiceBuilder.addDependency(moduleContextName, Context.class, resourceBinder.getContextInjector());
        binderServiceBuilder.addDependency(moduleContextName, Context.class, bindValue.getContextInjector());
        binderServiceBuilder.addOptionalDependency(resourceBinderBaseName.append(mappedName), bindValue.getValueInjector());
        return resourceInjection;
    }

    private boolean isPrimative(final String typeName) {
        return PRIMITIVE_NAME_TYPE_MAP.containsKey(typeName);
    }

    private Class<?> loadClass(final String name, final ClassLoader classLoader) throws ClassNotFoundException {
        if(PRIMITIVE_NAME_TYPE_MAP.containsKey(name)) {
            return PRIMITIVE_NAME_TYPE_MAP.get(name);
        }
        return classLoader.loadClass(name);
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
