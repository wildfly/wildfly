/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.handlers;

import static org.wildfly.extension.undertow.Capabilities.REF_SSL_CONTEXT;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_REVERSE_PROXY_HANDLER_HOST;
import static org.wildfly.extension.undertow.logging.UndertowLogger.ROOT_LOGGER;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;

import io.undertow.UndertowOptions;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowSubsystemModel;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.ssl.XnioSsl;

/**
 * @author Stuart Douglas
 * @author Tomaz Cerar
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ReverseProxyHandlerHostDefinition extends PersistentResourceDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.HOST);
    private static final RuntimeCapability<Void> REVERSE_PROXY_HOST_RUNTIME_CAPABILITY =
                RuntimeCapability.Builder.of(CAPABILITY_REVERSE_PROXY_HANDLER_HOST, true, ReverseProxyHostService.class)
                        .setDynamicNameMapper(BinaryCapabilityNameResolver.PARENT_CHILD)
                        .build();

    public static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder("outbound-socket-binding", ModelType.STRING)
            .setRequired(true)
            .setValidator(new StringLengthValidator(1, false))
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .setCapabilityReference(CapabilityReferenceRecorder.builder(REVERSE_PROXY_HOST_RUNTIME_CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build())
            .build();

    public static final AttributeDefinition SCHEME = new SimpleAttributeDefinitionBuilder("scheme", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("http"))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition PATH = new SimpleAttributeDefinitionBuilder("path", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("/"))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition INSTANCE_ID = new SimpleAttributeDefinitionBuilder(Constants.INSTANCE_ID, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition SSL_CONTEXT = new SimpleAttributeDefinitionBuilder(Constants.SSL_CONTEXT, ModelType.STRING, true)
            .setAlternatives(Constants.SECURITY_REALM)
            .setCapabilityReference(REF_SSL_CONTEXT)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
            .build();

    public static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_REALM, ModelType.STRING)
            .setAlternatives(Constants.SSL_CONTEXT)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF)
            .setDeprecated(UndertowSubsystemModel.VERSION_12_0_0.getVersion())
            .build();

    public static final SimpleAttributeDefinition ENABLE_HTTP2 = new SimpleAttributeDefinitionBuilder(Constants.ENABLE_HTTP2, ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(ModelNode.FALSE)
            .setRestartAllServices()
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(OUTBOUND_SOCKET_BINDING, SCHEME, INSTANCE_ID, PATH, SSL_CONTEXT, SECURITY_REALM, ENABLE_HTTP2);

    ReverseProxyHandlerHostDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(Constants.HANDLER, Constants.REVERSE_PROXY, PATH_ELEMENT.getKey()))
                .setCapabilities(REVERSE_PROXY_HOST_RUNTIME_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }


    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        ReverseProxyHostAdd add = new ReverseProxyHostAdd();
        registerAddOperation(resourceRegistration, add, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        registerRemoveOperation(resourceRegistration, new ServiceRemoveStepHandler(add) {
            @Override
            protected ServiceName serviceName(String name, final PathAddress address) {
                return REVERSE_PROXY_HOST_RUNTIME_CAPABILITY.getCapabilityServiceName(address);
            }
        }, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    private static class ReverseProxyHostAdd extends AbstractAddStepHandler {

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final PathAddress address = context.getCurrentAddress();
            final String proxyName = address.getElement(address.size() - 2).getValue();
            final String socketBinding = OUTBOUND_SOCKET_BINDING.resolveModelAttribute(context, model).asString();
            final String scheme = SCHEME.resolveModelAttribute(context, model).asString();
            final String path = PATH.resolveModelAttribute(context, model).asString();
            final boolean enableHttp2 = ENABLE_HTTP2.resolveModelAttribute(context, model).asBoolean();
            final String jvmRoute;
            final ModelNode securityRealm = SECURITY_REALM.resolveModelAttribute(context, model);
            if (securityRealm.isDefined()) {
                throw ROOT_LOGGER.runtimeSecurityRealmUnsupported();
            }
            final ModelNode sslContext = SSL_CONTEXT.resolveModelAttribute(context, model);
            if (model.hasDefined(Constants.INSTANCE_ID)) {
                jvmRoute = INSTANCE_ID.resolveModelAttribute(context, model).asString();
            } else {
                jvmRoute = null;
            }
            final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(REVERSE_PROXY_HOST_RUNTIME_CAPABILITY);
            final Consumer<ReverseProxyHostService> serviceConsumer = sb.provides(REVERSE_PROXY_HOST_RUNTIME_CAPABILITY);
            final Supplier<HttpHandler> phSupplier = sb.requiresCapability(Capabilities.CAPABILITY_HANDLER, HttpHandler.class, proxyName);
            final Supplier<OutboundSocketBinding> sbSupplier = sb.requires(OutboundSocketBinding.SERVICE_DESCRIPTOR, socketBinding);
            final Supplier<SSLContext> scSupplier = sslContext.isDefined() ? sb.requiresCapability(REF_SSL_CONTEXT, SSLContext.class, sslContext.asString()) : null;
            sb.setInstance(new ReverseProxyHostService(serviceConsumer, phSupplier, sbSupplier, scSupplier, scheme, jvmRoute, path, enableHttp2));
            sb.install();
        }
    }

    private static final class ReverseProxyHostService implements Service {

        private final Consumer<ReverseProxyHostService> serviceConsumer;
        private final Supplier<HttpHandler> proxyHandler;
        private final Supplier<OutboundSocketBinding> socketBinding;
        private final Supplier<SSLContext> sslContext;
        private final String instanceId;
        private final String scheme;
        private final String path;
        private final boolean enableHttp2;

        private ReverseProxyHostService(final Consumer<ReverseProxyHostService> serviceConsumer,
                final Supplier<HttpHandler> proxyHandler,
                final Supplier<OutboundSocketBinding> socketBinding,
                final Supplier<SSLContext> sslContext,
                String scheme, String instanceId, String path, boolean enableHttp2) {
            this.serviceConsumer = serviceConsumer;
            this.proxyHandler = proxyHandler;
            this.socketBinding = socketBinding;
            this.sslContext = sslContext;
            this.instanceId = instanceId;
            this.scheme = scheme;
            this.path = path;
            this.enableHttp2 = enableHttp2;
        }
        private URI getUri() throws URISyntaxException {
            OutboundSocketBinding binding = socketBinding.get();
            return new URI(scheme, null, binding.getUnresolvedDestinationAddress(), binding.getDestinationPort(), path, null, null);
        }

        @Override
        public void start(final StartContext startContext) throws StartException {
            //todo: this is a bit of a hack, as the proxy handler may be wrapped by a request controller handler for graceful shutdown
            ProxyHandler proxyHandler = (ProxyHandler) (this.proxyHandler.get() instanceof GlobalRequestControllerHandler ? ((GlobalRequestControllerHandler)this.proxyHandler.get()).getNext() : this.proxyHandler.get());

            final LoadBalancingProxyClient client = (LoadBalancingProxyClient) proxyHandler.getProxyClient();
            try {
                SSLContext sslContext = this.sslContext != null ? this.sslContext.get() : null;

                if (sslContext == null) {
                    client.addHost(getUri(), instanceId, null, OptionMap.create(UndertowOptions.ENABLE_HTTP2, enableHttp2));
                } else {
                    OptionMap.Builder builder = OptionMap.builder();
                    builder.set(Options.USE_DIRECT_BUFFERS, true);
                    OptionMap combined = builder.getMap();

                    XnioSsl xnioSsl = new UndertowXnioSsl(Xnio.getInstance(), combined, sslContext);
                    client.addHost(getUri(), instanceId, xnioSsl, OptionMap.create(UndertowOptions.ENABLE_HTTP2, enableHttp2));
                }
                serviceConsumer.accept(this);
            } catch (URISyntaxException e) {
                throw new StartException(e);
            }
        }

        @Override
        public void stop(final StopContext stopContext) {
            serviceConsumer.accept(null);
            ProxyHandler proxyHandler = (ProxyHandler) (this.proxyHandler.get() instanceof GlobalRequestControllerHandler ? ((GlobalRequestControllerHandler)this.proxyHandler.get()).getNext() : this.proxyHandler.get());
            final LoadBalancingProxyClient client = (LoadBalancingProxyClient) proxyHandler.getProxyClient();
            try {
                client.removeHost(getUri());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e); //impossible
            }
        }
    }

}
