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
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Deployment unit processors responsible for adding deployment items for each managed bean configuration.
 *
 * @author John E. Bailey
 */
public class ManagedBeanDeploymentProcessors implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(200L);

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
            if (postConstructMethodName != null) {
                postConstructMethod = beanClass.getMethod(postConstructMethodName);
            }
        } catch (NoSuchMethodException e) {
            throw new DeploymentUnitProcessingException("Failed to get PostConstruct method '" + postConstructMethodName + "' for managed bean type: " + beanClass.getName(), e, null);
        }

        final String preDestroyMethodName = managedBeanConfiguration.getPreDestroyMethod();
        Method preDestroyMethod = null;
        try {
            if (preDestroyMethodName != null) {
                preDestroyMethod = beanClass.getMethod(preDestroyMethodName);
            }
        } catch (NoSuchMethodException e) {
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

        final BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(ManagedBeanService.SERVICE_NAME.append(deploymentName, name), managedBeanService);

        for(ResourceInjectionConfiguration resourceInjectionConfiguration : managedBeanConfiguration.getResourceInjectionConfigurations()) {
            final String targetName = resourceInjectionConfiguration.getName();
            final String contextNameSuffix;
            final Resource resource;
            final ResourceInjection<Object> resourceInjection;
            if(ResourceInjectionConfiguration.TargetType.FIELD.equals(resourceInjectionConfiguration.getTargetType())) {
                final Field field;
                try {
                    field = managedBeanClass.getDeclaredField(targetName);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Failed to get field '" + targetName + "' from class '" + managedBeanClass +"'", e);
                }
                resource = field.getAnnotation(Resource.class);
                contextNameSuffix = field.getName();
                resourceInjection = new FieldResourceInjection<Object>(targetName);
            } else {
                final Method method;
                try {
                    method = managedBeanClass.getMethod(targetName);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Failed to get method '" + targetName + "' from class '" + managedBeanClass +"'", e);
                }
                resource = method.getAnnotation(Resource.class);
                final String methodName = method.getName();
                contextNameSuffix = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                resourceInjection = new MethodResourceInjection<Object>(targetName, resourceInjectionConfiguration.getInjectedType());
            }
            if(!resource.type().equals(Object.class)) {
                resourceInjectionConfiguration.setInjectedType(resource.type().getName());
            }
            final String contextName = !"".equals(resource.name()) ? resource.name() : managedBeanClass.getName() + "/" + contextNameSuffix;
            serviceBuilder.addDependency(ResourceBinder.MODULE_SERVICE_NAME.append(deploymentName, contextName), resourceInjection.getValueInjector());
            resourceInjections.add(resourceInjection);
        }

        // TODO: Get naming context and add a ResourceBinder for this managed bean
    }
}
