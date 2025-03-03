/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.net.ssl.SSLContext;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SecurityConfiguration;
import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * /subsystem=infinispan/remote-cache-container=X/component=security
 *
 * @author Radoslav Husar
 */
public class SecurityResourceDefinition extends ComponentResourceDefinition {

    public static final PathElement PATH = pathElement("security");

    static final UnaryServiceDescriptor<SecurityConfiguration> SERVICE_DESCRIPTOR = serviceDescriptor(PATH, SecurityConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        SSL_CONTEXT("ssl-context", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.SSL_CONTEXT).build())
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
                        .setValidator(new StringLengthValidator(1))
                        ;
            }
        },
        ;

        private final AttributeDefinition definition;

        Attribute(String attributeName, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(attributeName, type)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES))
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    SecurityResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(CAPABILITY))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String sslContextName = Attribute.SSL_CONTEXT.resolveModelAttribute(context, model).asStringOrNull();
        ServiceDependency<SSLContext> sslContext = (sslContextName != null) ? ServiceDependency.on(CommonServiceDescriptor.SSL_CONTEXT, sslContextName) : ServiceDependency.of(null);

        Supplier<SecurityConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public SecurityConfiguration get() {
                SSLContext context = sslContext.get();
                return new ConfigurationBuilder().security().ssl().hostnameValidation(false).sslContext(context).enabled(context != null).security().create();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory)
                .requires(sslContext)
                .build();
    }
}
