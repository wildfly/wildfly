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
import org.jboss.as.deployment.naming.ContextNames;
import org.jboss.as.deployment.naming.ContextService;
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
        if(managedBeanConfigurations == null)
            return; // Skip deployments with no managed beans.

        final BatchBuilder batchBuilder = context.getBatchBuilder();

        for(ManagedBeanConfiguration managedBeanConfiguration : managedBeanConfigurations.getConfigurations().values()) {
            processManagedBean(context.getName(), managedBeanConfiguration, batchBuilder);
        }
    }

    private void processManagedBean(final String deploymentName, final ManagedBeanConfiguration managedBeanConfiguration, final BatchBuilder batchBuilder) throws DeploymentUnitProcessingException {
        final Class<Object> beanClass = (Class<Object>)managedBeanConfiguration.getType();
        final String managedBeanName = managedBeanConfiguration.getName();

        final List<ResourceInjection<?>> resourceInjections = new ArrayList<ResourceInjection<?>>();
        final ManagedBeanService<Object> managedBeanService = new ManagedBeanService<Object>(beanClass, managedBeanConfiguration.getPostConstructMethod(), managedBeanConfiguration.getPreDestroyMethod(), resourceInjections);

        final ServiceName binderBase = ResourceBinder.MODULE_BINDER.append(deploymentName);
        final ServiceName moduleContextServiceName = ContextNames.GLOBAL_CONTEXT_SERVICE_NAME.append(deploymentName);

        final ServiceName managedBeanServiceName = ManagedBeanService.SERVICE_NAME.append(deploymentName, managedBeanName);
        final BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(managedBeanServiceName, managedBeanService);

        final ServiceName managedBeanContextName = moduleContextServiceName.append(managedBeanName, "ctx");
        for (ResourceInjectionConfiguration resourceInjectionConfiguration : managedBeanConfiguration.getResourceInjectionConfigurations()) {
            final ResourceInjection<Object> resourceInjection = processResourceInjection(resourceInjectionConfiguration, managedBeanName, batchBuilder, serviceBuilder, deploymentName, moduleContextServiceName);
            resourceInjections.add(resourceInjection);
        }

        final ContextService actualBeanContext = new ContextService(managedBeanName + "_ctx");
        batchBuilder.addService(managedBeanContextName, actualBeanContext)
            .addDependency(moduleContextServiceName, Context.class, actualBeanContext.getParentContextInjector());

        final Reference managedBeanFactoryReference = ManagedBeanObjectFactory.createReference(beanClass, managedBeanServiceName);
        final ResourceBinder<Reference> managedBeanFactoryBinder = new ResourceBinder<Reference>(managedBeanName, Values.immediateValue(managedBeanFactoryReference));
        batchBuilder.addService(binderBase.append(managedBeanName), managedBeanFactoryBinder)
            .addDependency(moduleContextServiceName, Context.class, managedBeanFactoryBinder.getContextInjector());
    }

    private ResourceInjection<Object> processResourceInjection(final ResourceInjectionConfiguration resourceInjectionConfiguration, final String mangedBeanName, final BatchBuilder batchBuilder, final BatchServiceBuilder serviceBuilder, final String deploymentName, final ServiceName moduleContextServiceName) throws DeploymentUnitProcessingException {
        final String localContextName = resourceInjectionConfiguration.getLocalContextName();
        final String targetContextName = resourceInjectionConfiguration.getTargetContextName();

        final ResourceInjection<Object> resourceInjection = getResourceInjection(resourceInjectionConfiguration);

        // Now add a binder for the local context
        final ServiceName resourceBinderBaseName = ResourceBinder.MODULE_BINDER.append(deploymentName);
        final ServiceName binderName = resourceBinderBaseName.append(localContextName);
        serviceBuilder.addDependency(binderName, resourceInjection.getValueInjector());

        final LinkRef linkRef = new LinkRef(targetContextName.startsWith("java") ? targetContextName : ContextNames.MODULE_CONTEXT_NAME + "/" + targetContextName);
        final ResourceBinder<LinkRef> resourceBinder = new ResourceBinder<LinkRef>(localContextName, Values.immediateValue(linkRef));
        final BatchServiceBuilder<Object> binderServiceBuilder = batchBuilder.addService(binderName, resourceBinder);
        binderServiceBuilder.addDependency(moduleContextServiceName, Context.class, resourceBinder.getContextInjector());
        binderServiceBuilder.addDependency(resourceBinderBaseName.append(mangedBeanName));

        if(targetContextName.startsWith("java:")) {
            binderServiceBuilder.addOptionalDependency(ResourceBinder.JAVA_BINDER.append(targetContextName));
        } else {
            binderServiceBuilder.addOptionalDependency(resourceBinderBaseName.append(targetContextName));
        }

        return resourceInjection;
    }

    private ResourceInjection<Object> getResourceInjection(final ResourceInjectionConfiguration resourceInjectionConfiguration) {
        if(ResourceInjectionConfiguration.TargetType.FIELD.equals(resourceInjectionConfiguration.getTargetType())) {
            return new FieldResourceInjection<Object>(Values.immediateValue(Field.class.cast(resourceInjectionConfiguration.getTarget())), resourceInjectionConfiguration.getInjectedType().isPrimitive());
        } else {
            return new MethodResourceInjection<Object>(Values.immediateValue(Method.class.cast(resourceInjectionConfiguration.getTarget())), resourceInjectionConfiguration.getInjectedType().isPrimitive());
        }
    }
}
