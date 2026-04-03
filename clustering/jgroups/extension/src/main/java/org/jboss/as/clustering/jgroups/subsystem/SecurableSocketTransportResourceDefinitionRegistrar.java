/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.BasicTCP;
import org.wildfly.clustering.jgroups.spi.TLSConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Extends {@link SocketTransportResourceDefinitionRegistrar} with TLS support for a socket transport.
 *
 * @author Radoslav Husar
 */
public class SecurableSocketTransportResourceDefinitionRegistrar<T extends BasicTCP> extends SocketTransportResourceDefinitionRegistrar<T> {

    enum Transport implements ResourceRegistration {
        TCP,
        ;
        private final PathElement path = StackResourceDefinitionRegistrar.Component.TRANSPORT.pathElement(this.name());

        @Override
        public PathElement getPathElement() {
            return this.path;
        }
    }

    private static final String CLIENT_SSL_CONTEXT_NAME = "client-ssl-context";
    private static final String SERVER_SSL_CONTEXT_NAME = "server-ssl-context";

    static final CapabilityReferenceAttributeDefinition<SSLContext> CLIENT_SSL_CONTEXT = new CapabilityReferenceAttributeDefinition.Builder<>(CLIENT_SSL_CONTEXT_NAME, CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.SSL_CONTEXT).build())
            .setRequired(false)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
            .setRequires(SERVER_SSL_CONTEXT_NAME)
            .setXmlName("client")
            .build();

    static final CapabilityReferenceAttributeDefinition<SSLContext> SERVER_SSL_CONTEXT = new CapabilityReferenceAttributeDefinition.Builder<>(SERVER_SSL_CONTEXT_NAME, CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.SSL_CONTEXT).build())
            .setRequired(false)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
            .setRequires(CLIENT_SSL_CONTEXT_NAME)
            .setXmlName("server")
            .build();

    SecurableSocketTransportResourceDefinitionRegistrar(Transport registration, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(registration, parentRuntimeHandler);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(CLIENT_SSL_CONTEXT, SERVER_SSL_CONTEXT));
    }

    @Override
    public ServiceDependency<TransportConfiguration<T>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<TransportConfiguration<T>> configuration = super.resolve(context, model);
        ServiceDependency<SSLContext> clientSSLContext = CLIENT_SSL_CONTEXT.resolve(context, model);
        ServiceDependency<SSLContext> serverSSLContext = SERVER_SSL_CONTEXT.resolve(context, model);

        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                configuration.accept(builder);

                clientSSLContext.accept(builder);
                serverSSLContext.accept(builder);
            }

            @Override
            public TransportConfiguration<T> get() {
                TransportConfiguration<T> transportConfiguration = configuration.get();
                return new TransportConfigurationDecorator<>(transportConfiguration) {
                    @Override
                    public Optional<TLSConfiguration> getSSLConfiguration() {
                        if (serverSSLContext.isPresent() && clientSSLContext.isPresent()) {
                            return Optional.of(new TLSConfiguration() {
                                @Override
                                public SSLContext getClientSSLContext() {
                                    return clientSSLContext.get();
                                }

                                @Override
                                public SSLContext getServerSSLContext() {
                                    return serverSSLContext.get();
                                }
                            });
                        } else {
                            return Optional.empty();
                        }
                    }
                };
            }
        };
    }
}
