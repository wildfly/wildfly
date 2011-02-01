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

package org.jboss.as.ee.component.processor;

import java.util.List;
import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.Reference;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.ee.component.injection.ResourceInjectionResolver;
import org.jboss.as.ee.component.service.ComponentObjectFactory;
import org.jboss.as.ee.component.service.ComponentService;
import org.jboss.as.naming.ServiceReferenceObjectFactory;
import org.jboss.as.naming.deployment.ContextService;
import org.jboss.as.naming.deployment.ResourceBinder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Values;

/**
 * Deployment processor responsible for converting {@link org.jboss.as.ee.component.ComponentConfiguration} instances into {@link org.jboss.as.ee.component.Component}instances.
 *
 * @author John Bailey
 */
public class ComponentInstallProcessor extends AbstractComponentConfigProcessor {

    /**
     * {@inheritDoc}
     */
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final ComponentFactory componentFactory = componentConfiguration.getAttachment(Attachments.COMPONENT_FACTORY);

        // Create the component
        final ComponentFactory.ConstructedComponent constructedComponent = componentFactory.createComponent(deploymentUnit, componentConfiguration);

        // Add the required services
        final ServiceName beanEnvContextServiceName = constructedComponent.getEnvContextServiceName().append(componentConfiguration.getName());
        final ContextService actualBeanContext = new ContextService(componentConfiguration.getName());
        serviceTarget.addService(beanEnvContextServiceName, actualBeanContext)
                .addDependency(constructedComponent.getEnvContextServiceName(), Context.class, actualBeanContext.getParentContextInjector())
                .install();

        final ServiceName bindContextServiceName = constructedComponent.getBindContextServiceName();
        final Reference componentFactoryReference = ServiceReferenceObjectFactory.createReference(constructedComponent.getComponentServiceName(), ComponentObjectFactory.class);
        final ResourceBinder<Reference> factoryBinder = new ResourceBinder<Reference>(constructedComponent.getBindName(), Values.immediateValue(componentFactoryReference));
        final ServiceName referenceBinderName = bindContextServiceName.append(constructedComponent.getBindName());
        serviceTarget.addService(referenceBinderName, factoryBinder)
                .addDependency(bindContextServiceName, Context.class, factoryBinder.getContextInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();

        final ComponentService componentService = new ComponentService(constructedComponent.getComponent());
        final ServiceBuilder<?> serviceBuilder = serviceTarget.addService(constructedComponent.getComponentServiceName(), componentService)
                .addDependency(referenceBinderName)
                .addDependency(constructedComponent.getCompContextServiceName(), Context.class, componentService.getCompContextInjector())
                .addDependency(constructedComponent.getModuleContextServiceName(), Context.class, componentService.getModuleContextInjector())
                .addDependency(constructedComponent.getAppContextServiceName(), Context.class, componentService.getAppContextInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE);

        // Make sure all the dependencies are in place for the component's resource injections
        final List<ResourceInjectionResolver.ResolverResult> resolverResults = componentConfiguration.getAttachment(Attachments.RESOLVED_RESOURCES);
        if (resolverResults != null) for (ResourceInjectionResolver.ResolverResult resolverResult : resolverResults) {
            for (ResourceInjectionResolver.ResolverDependency<?> dependency : resolverResult.getDependencies()) {
                addDependency(serviceBuilder, dependency);
            }
            if (resolverResult.shouldBind()) {
                addDependency(serviceBuilder, bindResource(serviceTarget, resolverResult));
            }
        }
        serviceBuilder.install();
    }


    private ResourceInjectionResolver.ResolverDependency<?> bindResource(final ServiceTarget serviceTarget, final ResourceInjectionResolver.ResolverResult resolverResult) throws DeploymentUnitProcessingException {
        final ServiceName binderName = resolverResult.getBindContextName().append(resolverResult.getBindName());

        final LinkRef linkRef = new LinkRef(resolverResult.getBindTargetName());
        final ResourceBinder<LinkRef> resourceBinder = new ResourceBinder<LinkRef>(resolverResult.getBindName(), Values.immediateValue(linkRef));

        serviceTarget.addService(binderName, resourceBinder)
                .addDependency(resolverResult.getBindContextName(), Context.class, resourceBinder.getContextInjector())
                .install();

        return new ResourceInjectionResolver.ResolverDependency<Object>() {
            public ServiceName getServiceName() {
                return binderName;
            }

            public Injector<Object> getInjector() {
                return null;
            }

            public Class<Object> getInjectorType() {
                return null;
            }
        };
    }

    private <T> void addDependency(final ServiceBuilder<?> serviceBuilder, final ResourceInjectionResolver.ResolverDependency<T> dependency) {
        if (dependency.getInjector() != null) {
            serviceBuilder.addDependency(dependency.getServiceName(), dependency.getInjectorType(), dependency.getInjector());
        } else {
            serviceBuilder.addDependency(dependency.getServiceName());
        }
    }
}
