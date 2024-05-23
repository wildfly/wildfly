/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_HTTP_INVOKER_HOST;
import static org.wildfly.extension.undertow.UndertowRootDefinition.HTTP_INVOKER_RUNTIME_CAPABILITY;
import static org.wildfly.extension.undertow.logging.UndertowLogger.ROOT_LOGGER;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import io.undertow.server.handlers.PathHandler;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class HttpInvokerDefinition extends PersistentResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.SETTING, Constants.HTTP_INVOKER);
    static final RuntimeCapability<Void> HTTP_INVOKER_HOST_CAPABILITY =
                RuntimeCapability.Builder.of(CAPABILITY_HTTP_INVOKER_HOST, true, Void.class)
                        .setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT)
                        .addRequirements(Capabilities.CAPABILITY_HTTP_INVOKER)
                        .build();

    static final SimpleAttributeDefinition HTTP_AUTHENTICATION_FACTORY = new SimpleAttributeDefinitionBuilder(Constants.HTTP_AUTHENTICATION_FACTORY, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, true))
            .setRestartAllServices()
            .setCapabilityReference(Capabilities.REF_HTTP_AUTHENTICATION_FACTORY)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.AUTHENTICATION_FACTORY_REF)
            .setAlternatives(Constants.SECURITY_REALM)
            .build();


    protected static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_REALM, ModelType.STRING, true)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF)
            .setAlternatives(Constants.HTTP_AUTHENTICATION_FACTORY)
            .setDeprecated(UndertowSubsystemModel.VERSION_12_0_0.getVersion())
            .build();

    protected static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("wildfly-services"))
            .setRestartAllServices()
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = Arrays.asList(
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            PATH,
            HTTP_AUTHENTICATION_FACTORY,
            SECURITY_REALM
    );

    HttpInvokerDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getValue()))
                .setAddHandler(HttpInvokerAdd.INSTANCE)
                .setRemoveHandler(new ServiceRemoveStepHandler(HttpInvokerAdd.INSTANCE))
                .setCapabilities(HTTP_INVOKER_HOST_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    private static final class HttpInvokerAdd extends AbstractAddStepHandler {
        static final AbstractAddStepHandler INSTANCE = new HttpInvokerAdd();

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final PathAddress address = context.getCurrentAddress();
            final PathAddress hostAddress = address.getParent();
            final PathAddress serverAddress = hostAddress.getParent();
            String path = PATH.resolveModelAttribute(context, model).asString();
            String httpAuthenticationFactory = null;
            final ModelNode authFactory = HTTP_AUTHENTICATION_FACTORY.resolveModelAttribute(context, model);
            final ModelNode securityRealm = SECURITY_REALM.resolveModelAttribute(context, model);
            if (authFactory.isDefined()) {
                httpAuthenticationFactory = authFactory.asString();
            } else if (securityRealm.isDefined()) {
                throw ROOT_LOGGER.runtimeSecurityRealmUnsupported();
            }

            final String serverName = serverAddress.getLastElement().getValue();
            final String hostName = hostAddress.getLastElement().getValue();

            final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(HTTP_INVOKER_HOST_CAPABILITY);
            final Supplier<Host> hSupplier = sb.requires(Host.SERVICE_DESCRIPTOR, serverName, hostName);
            Supplier<HttpAuthenticationFactory> hafSupplier = null;
            final Supplier<PathHandler> phSupplier = sb.requiresCapability(HTTP_INVOKER_RUNTIME_CAPABILITY.getName(), PathHandler.class);
            if (httpAuthenticationFactory != null) {
                hafSupplier = sb.requiresCapability(Capabilities.REF_HTTP_AUTHENTICATION_FACTORY, HttpAuthenticationFactory.class, httpAuthenticationFactory);
            }
            sb.setInstance(new HttpInvokerHostService(hSupplier, hafSupplier, phSupplier, path));
            sb.install();
        }
    }
}
