/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.resource.definition;

import jakarta.enterprise.concurrent.ContextService;
import org.jboss.as.ee.concurrent.ContextServiceTypesConfiguration;
import org.jboss.as.ee.concurrent.service.ContextServiceService;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.ee.subsystem.ContextServiceResourceDefinition;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * The {@link ResourceDefinitionInjectionSource} for {@link jakarta.enterprise.concurrent.ContextServiceDefinition}
 *
 * @author emmartins
 */
public class ContextServiceDefinitionInjectionSource extends ResourceDefinitionInjectionSource {

    private final ContextServiceTypesConfiguration contextServiceTypesConfiguration;

    public ContextServiceDefinitionInjectionSource(final String jndiName, final ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        super(jndiName);
        this.contextServiceTypesConfiguration = contextServiceTypesConfiguration;
    }

    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final String resourceName = uniqueName(context);
        final String resourceJndiName = "java:jboss/ee/concurrency/definition/context/"+resourceName;
        try {
            // install the resource service
            final ContextServiceService resourceService = new ContextServiceService(resourceName, resourceJndiName, contextServiceTypesConfiguration);
            final ServiceName resourceServiceName = ContextServiceResourceDefinition.CAPABILITY.getCapabilityServiceName(resourceName);
            phaseContext.getServiceTarget()
                    .addService(resourceServiceName)
                    .setInstance(resourceService)
                    .install();
            // use a dependency to the resource service installed to inject the resource
            serviceBuilder.addDependency(resourceServiceName, ContextService.class, new Injector<>() {
                @Override
                public void inject(final ContextService resource) throws InjectionException {
                    injector.inject(() -> new ManagedReference() {
                        @Override
                        public void release() {
                        }
                        @Override
                        public Object getInstance() {
                            return resource;
                        }
                    });
                }
                @Override
                public void uninject() {
                    injector.uninject();
                }
            });
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    public ContextServiceTypesConfiguration getContextServiceTypesConfiguration() {
        return contextServiceTypesConfiguration;
    }
}
