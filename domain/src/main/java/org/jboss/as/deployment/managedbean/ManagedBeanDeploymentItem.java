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

import org.jboss.as.deployment.item.DeploymentItem;
import org.jboss.as.deployment.item.DeploymentItemContext;
import org.jboss.as.deployment.module.DeploymentModuleService;
import org.jboss.as.deployment.module.ModuleClassLoaderTranslator;
import org.jboss.modules.Module;
import org.jboss.msc.inject.TranslatingInjector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Deployment item responsible for taking managed bean configuration and converting it into service definitions.
 *
 * @author John E. Bailey
 */
public class ManagedBeanDeploymentItem implements DeploymentItem {
    private static final long serialVersionUID = 7706399887995664349L;
    private final String deploymentName;
    private final ManagedBeanConfiguration managedBeanConfiguration;

    /**
     * Construct with a deployment name and ManagedBeanConfiguration.
     *
     * @param deploymentName The deployment name
     * @param managedBeanConfiguration The managed bean configuration
     */
    public ManagedBeanDeploymentItem(String deploymentName, ManagedBeanConfiguration managedBeanConfiguration) {
        this.deploymentName = deploymentName;
        this.managedBeanConfiguration = managedBeanConfiguration;
    }

    /**
     * Use the managedBeanConfiguration to install the necessary service to support this managed bean.
     * 
     * @param context the context used for this item execution
     */
    public void install(final DeploymentItemContext context) {
        final BatchBuilder batchBuilder = context.getBatchBuilder();
        final ManagedBeanService<Object> managedBeanService = new ManagedBeanService<Object>(managedBeanConfiguration);

        final ClassLoader classLoader = getMagicClassLoader();
        final Class<?> managedBeanClass;
        try {
            managedBeanClass = classLoader.loadClass(managedBeanConfiguration.getType());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load managed bean class", e);
        }
        final ManagedBean managedBeanAnnotation = managedBeanClass.getAnnotation(ManagedBean.class);
        final String name = managedBeanAnnotation.value();
        managedBeanConfiguration.setName(name);

        final BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(ManagedBeanService.SERVICE_NAME.append(deploymentName, name), managedBeanService);
        serviceBuilder.addDependency(DeploymentModuleService.SERVICE_NAME.append(deploymentName), Module.class, new TranslatingInjector<Module, ClassLoader>(new ModuleClassLoaderTranslator(), managedBeanService.getClassLoaderInjector()));

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
            managedBeanService.addResourceInjection(resourceInjection);
        }

        // TODO: Get naming context and add a ResourceBinder for this managed bean
    }

    private ClassLoader getMagicClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
