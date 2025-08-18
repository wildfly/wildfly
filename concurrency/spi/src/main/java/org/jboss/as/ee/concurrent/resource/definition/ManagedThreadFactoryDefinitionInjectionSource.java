/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.resource.definition;

import java.util.function.Consumer;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.concurrent.WildFlyManagedThreadFactory;
import org.jboss.as.ee.concurrent.WildFlyContextService;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentDefaultBindingProcessor;
import org.jboss.as.ee.concurrent.service.ManagedThreadFactoryService;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.ee.subsystem.ManagedThreadFactoryResourceDefinition;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * The {@link ResourceDefinitionInjectionSource} for {@link jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition}.
 *
 * @author emmartins
 */
public class ManagedThreadFactoryDefinitionInjectionSource extends ResourceDefinitionInjectionSource {

    public static final String CONTEXT_PROP = "context";
    public static final String PRIORITY_PROP = "priority";

    private String contextServiceRef;
    private int priority = Thread.NORM_PRIORITY;

    public ManagedThreadFactoryDefinitionInjectionSource(final String jndiName) {
        super(jndiName);
    }

    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final String resourceName = uniqueName(context);
        final String resourceJndiName = "java:jboss/ee/concurrency/definition/managedThreadFactory/"+resourceName;
        try {
            // install the resource service
            final ServiceName resourceServiceName = ManagedThreadFactoryResourceDefinition.CAPABILITY.getCapabilityServiceName(resourceName);
            final ServiceBuilder resourceServiceBuilder = phaseContext.getServiceTarget().addService(resourceServiceName);
            final Consumer<WildFlyManagedThreadFactory> consumer = resourceServiceBuilder.provides(resourceServiceName);
            final ManagedThreadFactoryService resourceService = new ManagedThreadFactoryService(consumer, null, resourceName, resourceJndiName, priority);
            final Injector<ManagedReferenceFactory> contextServiceLookupInjector = new Injector<>() {
                @Override
                public void inject(ManagedReferenceFactory value) throws InjectionException {
                    resourceService.getContextServiceSupplier().set(() -> (WildFlyContextService) value.getReference().getInstance());
                }
                @Override
                public void uninject() {
                    resourceService.getContextServiceSupplier().set(() -> null);
                }
            };
            final String contextServiceRef;
            if (this.contextServiceRef == null || this.contextServiceRef.isEmpty() || this.contextServiceRef.equals(EEConcurrentDefaultBindingProcessor.COMP_DEFAULT_CONTEXT_SERVICE_JNDI_NAME)) {
                // default context service, use the real name of the resource since java:comp may not exist (e.g. ear)
                final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
                final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
                contextServiceRef = moduleDescription.getDefaultResourceJndiNames().getContextService();
            } else {
                contextServiceRef = this.contextServiceRef;
            }
            final ContextNames.BindInfo contextServiceBindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), contextServiceRef);
            contextServiceBindInfo.setupLookupInjection(resourceServiceBuilder, contextServiceLookupInjector, phaseContext.getDeploymentUnit(), false);
            resourceServiceBuilder.setInstance(resourceService);
            resourceServiceBuilder.install();
            // use a dependency to the resource service installed to inject the resource
            serviceBuilder.addDependency(resourceServiceName, WildFlyManagedThreadFactory.class, new Injector<>() {
                @Override
                public void inject(final WildFlyManagedThreadFactory resource) throws InjectionException {
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

    public String getContextServiceRef() {
        return contextServiceRef;
    }

    public void setContextServiceRef(String contextServiceRef) {
        this.contextServiceRef = contextServiceRef;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
