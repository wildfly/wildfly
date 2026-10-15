/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.web.host.WebHost;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostDefinition extends SimpleResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.HOST);
    public static final String DEFAULT_WEB_MODULE_DEFAULT = "ROOT.war";

    static final RuntimeCapability<Void> HOST_CAPABILITY = RuntimeCapability.Builder.of(Host.SERVICE_DESCRIPTOR).build();
    static final RuntimeCapability<Void> DEFAULT_SERVER_HOST_CAPABILITY = RuntimeCapability.Builder.of(Host.DEFAULT_SERVER_SERVICE_DESCRIPTOR).build();

    static final StringListAttributeDefinition ALIAS = new StringListAttributeDefinition.Builder(Constants.ALIAS)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setElementValidator(new StringLengthValidator(1))
            .setAllowExpression(true)
            .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
            .build();
    static final SimpleAttributeDefinition DEFAULT_WEB_MODULE = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_WEB_MODULE, ModelType.STRING, true)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1, true, false))
            .setDefaultValue(new ModelNode(DEFAULT_WEB_MODULE_DEFAULT))
            .build();

    static final SimpleAttributeDefinition DEFAULT_RESPONSE_CODE = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_RESPONSE_CODE, ModelType.INT, true)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(400, 599, true, true))
            .setDefaultValue(new ModelNode(404))
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition DISABLE_CONSOLE_REDIRECT = new SimpleAttributeDefinitionBuilder("disable-console-redirect", ModelType.BOOLEAN, true)
            .setRestartAllServices()
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition QUEUE_REQUESTS_ON_START = new SimpleAttributeDefinitionBuilder("queue-requests-on-start", ModelType.BOOLEAN, true)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(ALIAS, DEFAULT_WEB_MODULE, DEFAULT_RESPONSE_CODE, DISABLE_CONSOLE_REDIRECT, QUEUE_REQUESTS_ON_START);

    static final BiPredicate<OperationContext, Resource> HOST_OF_DEFAULT_SERVER = new BiPredicate<OperationContext, Resource>() {
        @Override
        public boolean test(OperationContext context, Resource resource) {
            PathAddress hostAddress = context.getCurrentAddress();
            PathAddress serverAddress = hostAddress.getParent();
            try {
                String defaultServer = UndertowRootDefinition.DEFAULT_SERVER.resolveModelAttribute(context, context.readResourceFromRoot(serverAddress.getParent(), false).getModel()).asStringOrNull();
                return serverAddress.getLastElement().getValue().equals(defaultServer);
            } catch (OperationFailedException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    private final ResourceDescriptor descriptor;

    HostDefinition() {
        this(ResourceDescriptor.builder(UndertowExtension.getResolver(PATH_ELEMENT.getKey()))
                .addAttributes(ATTRIBUTES)
                .addCapability(HOST_CAPABILITY)
                // Register these capabilities for the default server only
                .addCapabilities(List.of(DEFAULT_SERVER_HOST_CAPABILITY, WebHost.CAPABILITY), HOST_OF_DEFAULT_SERVER)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(HostServiceConfigurator.INSTANCE))
                .addResourceCapabilityReference(ResourceCapabilityReference.builder(HOST_CAPABILITY, Server.SERVICE_DESCRIPTOR).withRequirementNameResolver(UnaryCapabilityNameResolver.PARENT).build())
                .build());
    }

    private HostDefinition(ResourceDescriptor descriptor) {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, descriptor.getResourceDescriptionResolver()));
        this.descriptor = descriptor;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        ManagementResourceRegistrar.of(this.descriptor).register(registration);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new LocationDefinition());
        registration.registerSubModel(new AccessLogDefinition());
        registration.registerSubModel(new ConsoleAccessLogDefinition());
        registration.registerSubModel(new FilterRefDefinition(true));
        registration.registerSubModel(new HttpInvokerDefinition());
        new HostSingleSignOnDefinition().register(registration, null);
    }
}
