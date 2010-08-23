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
import org.jboss.as.deployment.naming.ContextService;
import org.jboss.as.deployment.naming.ModuleContextConfig;
import org.jboss.as.deployment.naming.ResourceBinder;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Values;

import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Deployment unit processors responsible for adding deployment items for each managed bean configuration.
 *
 * @author John E. Bailey
 */
public class ManagedBeanDeploymentProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(200L);

    /**
     * Process the deployment and add a managed bean service for each managed bean configuration in the deployment.
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final ManagedBeanConfigurations managedBeanConfigurations = context.getAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY);
        if(managedBeanConfigurations == null) {
            return; // Skip deployments with no managed beans.
        }
        final ModuleContextConfig moduleContext = context.getAttachment(ModuleContextConfig.ATTACHMENT_KEY);
        if(moduleContext == null) {
            throw new DeploymentUnitProcessingException("Unable to deploy managed beans without a module naming context");
        }
        
        final BatchBuilder batchBuilder = context.getBatchBuilder();

        for(ManagedBeanConfiguration managedBeanConfiguration : managedBeanConfigurations.getConfigurations().values()) {
            processManagedBean(context, moduleContext, managedBeanConfiguration, batchBuilder);
        }
    }

    private void processManagedBean(final DeploymentUnitContext deploymentContext, final ModuleContextConfig moduleContext, final ManagedBeanConfiguration managedBeanConfiguration, final BatchBuilder batchBuilder) throws DeploymentUnitProcessingException {
        final Class<Object> beanClass = (Class<Object>)managedBeanConfiguration.getType();
        final String managedBeanName = managedBeanConfiguration.getName();

        final List<ResourceInjection<?>> resourceInjections = new ArrayList<ResourceInjection<?>>();
        final ManagedBeanService<Object> managedBeanService = new ManagedBeanService<Object>(beanClass, managedBeanConfiguration.getPostConstructMethod(), managedBeanConfiguration.getPreDestroyMethod(), resourceInjections);

        final ServiceName moduleContextServiceName = moduleContext.getContextServiceName();

        final ServiceName managedBeanServiceName = ManagedBeanService.SERVICE_NAME.append(deploymentContext.getName(), managedBeanName);
        final BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(managedBeanServiceName, managedBeanService);

        final ServiceName managedBeanContextName = moduleContextServiceName.append(managedBeanName, "context");
        for (ResourceConfiguration resourceConfiguration : managedBeanConfiguration.getResourceInjectionConfigurations()) {
            final ResourceInjection<Object> resourceInjection = processResource(moduleContext, resourceConfiguration, batchBuilder, serviceBuilder, managedBeanContextName);
            if(resourceInjection != null) {
                resourceInjections.add(resourceInjection);
            }
        }

        final ContextService actualBeanContext = new ContextService(managedBeanName + "-context");
        batchBuilder.addService(managedBeanContextName, actualBeanContext)
            .addDependency(moduleContextServiceName, Context.class, actualBeanContext.getParentContextInjector());

        final Reference managedBeanFactoryReference = ManagedBeanObjectFactory.createReference(beanClass, managedBeanServiceName);
        final ResourceBinder<Reference> managedBeanFactoryBinder = new ResourceBinder<Reference>(managedBeanName, Values.immediateValue(managedBeanFactoryReference));
        final ServiceName referenceBinderName = moduleContextServiceName.append(managedBeanName);
        batchBuilder.addService(referenceBinderName, managedBeanFactoryBinder)
            .addDependency(moduleContextServiceName, Context.class, managedBeanFactoryBinder.getContextInjector())
            .addDependency(managedBeanServiceName);
    }

    private ResourceInjection<Object> processResource(final ModuleContextConfig moduleContext, final ResourceConfiguration resourceConfiguration, final BatchBuilder batchBuilder, final BatchServiceBuilder serviceBuilder, final ServiceName beanContextServiceName) throws DeploymentUnitProcessingException {
        final String localContextName = resourceConfiguration.getLocalContextName();
        final String targetContextName = resourceConfiguration.getTargetContextName();

        final ResourceInjection<Object> resourceInjection = getResourceInjection(resourceConfiguration);

        // Now add a binder for the local context
        final ServiceName binderName = beanContextServiceName.append(localContextName);
        if(resourceInjection != null) {
            serviceBuilder.addDependency(binderName, resourceInjection.getValueInjector());
        }

        // TODO use java:module based name
        final LinkRef linkRef = new LinkRef(targetContextName.startsWith("java") ? targetContextName : moduleContext.getContextName() + "/" + targetContextName);
        final ResourceBinder<LinkRef> resourceBinder = new ResourceBinder<LinkRef>(localContextName, Values.immediateValue(linkRef));

        final BatchServiceBuilder<Object> binderServiceBuilder = batchBuilder.addService(binderName, resourceBinder);
        binderServiceBuilder.addDependency(beanContextServiceName, Context.class, resourceBinder.getContextInjector());

        if(targetContextName.startsWith("java:")) {
            binderServiceBuilder.addOptionalDependency(ResourceBinder.JAVA_BINDER.append(targetContextName));
        } else {
            binderServiceBuilder.addOptionalDependency(moduleContext.getContextServiceName().append(targetContextName));
        }

        return resourceInjection;
    }

    private ResourceInjection<Object> getResourceInjection(final ResourceConfiguration resourceConfiguration) {
        switch(resourceConfiguration.getTargetType()) {
            case FIELD:
                return new FieldResourceInjection<Object>(Values.immediateValue(Field.class.cast(resourceConfiguration.getTarget())), resourceConfiguration.getInjectedType().isPrimitive());
            case METHOD:
                return new MethodResourceInjection<Object>(Values.immediateValue(Method.class.cast(resourceConfiguration.getTarget())), resourceConfiguration.getInjectedType().isPrimitive());
            default:
                return null;
        }
    }
}
