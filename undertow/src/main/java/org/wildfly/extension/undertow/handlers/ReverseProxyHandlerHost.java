/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.handlers;

import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.GlobalRequestControllerHandler;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.ssl.XnioSsl;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Stuart Douglas
 * @author Tomaz Cerar
 */
public class ReverseProxyHandlerHost extends PersistentResourceDefinition {

    private static final String REVERSE_PROXY_HANDLER_HOST_CAPABILITY_NAME = "org.wildfly.undertow.reverse-proxy.host";
    private static final String SSL_CONTEXT_CAPABILITY = "org.wildfly.security.ssl-context";

    public static final ReverseProxyHandlerHost INSTANCE = new ReverseProxyHandlerHost();

    public static final ServiceName SERVICE_NAME = UndertowService.HANDLER.append("reverse-proxy", "host");


    public static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder("outbound-socket-binding", ModelType.STRING, true) //todo consider what we can do to make this non nullable
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    public static final AttributeDefinition SCHEME = new SimpleAttributeDefinitionBuilder("scheme", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("http"))
            .build();

    public static final AttributeDefinition PATH = new SimpleAttributeDefinitionBuilder("path", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("/"))
            .build();

    public static final AttributeDefinition INSTANCE_ID = new SimpleAttributeDefinitionBuilder(Constants.INSTANCE_ID, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SSL_CONTEXT = new SimpleAttributeDefinitionBuilder(Constants.SSL_CONTEXT, ModelType.STRING, true)
            .setAlternatives(Constants.SECURITY_REALM)
            .setCapabilityReference(SSL_CONTEXT_CAPABILITY, REVERSE_PROXY_HANDLER_HOST_CAPABILITY_NAME, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .build();

    public static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_REALM, ModelType.STRING)
            .setAlternatives(Constants.SSL_CONTEXT)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF)
            .build();

    private ReverseProxyHandlerHost() {
        super(PathElement.pathElement(Constants.HOST), UndertowExtension.getResolver(Constants.HANDLER, Constants.REVERSE_PROXY, Constants.HOST));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(OUTBOUND_SOCKET_BINDING, SCHEME, INSTANCE_ID, PATH, SSL_CONTEXT, SECURITY_REALM);
    }


    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        ReverseProxyHostAdd add = new ReverseProxyHostAdd();
        registerAddOperation(resourceRegistration, add, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        registerRemoveOperation(resourceRegistration, new ServiceRemoveStepHandler(SERVICE_NAME, add) {
            @Override
            protected ServiceName serviceName(String name, final PathAddress address) {
                final String proxyName = address.getElement(address.size() - 2).getValue();
                return SERVICE_NAME.append(proxyName).append(name);
            }
        }, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);

    }


    private final class ReverseProxyHostAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition def : getAttributes()) {
                def.validateAndSet(operation, model);
            }
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            final String name = address.getLastElement().getValue();
            final String proxyName = address.getElement(address.size() - 2).getValue();
            final String socketBinding = OUTBOUND_SOCKET_BINDING.resolveModelAttribute(context, model).asString();
            final String scheme = SCHEME.resolveModelAttribute(context, model).asString();
            final String path = PATH.resolveModelAttribute(context, model).asString();
            final String jvmRoute;
            final ModelNode securityRealm = SECURITY_REALM.resolveModelAttribute(context, model);
            final ModelNode sslContext = SSL_CONTEXT.resolveModelAttribute(context, model);
            if (model.hasDefined(Constants.INSTANCE_ID)) {
                jvmRoute = INSTANCE_ID.resolveModelAttribute(context, model).asString();
            } else {
                jvmRoute = null;
            }
            ReverseProxyHostService service = new ReverseProxyHostService(scheme, jvmRoute, path);
            ServiceBuilder<ReverseProxyHostService> builder = context.getServiceTarget().addService(SERVICE_NAME.append(proxyName).append(name), service)
                    .addDependency(UndertowService.HANDLER.append(proxyName), HttpHandler.class, service.proxyHandler)
                    .addDependency(OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(socketBinding), OutboundSocketBinding.class, service.socketBinding);

            if (sslContext.isDefined()) {
                builder.addDependency(
                        context.getCapabilityServiceName(SSL_CONTEXT_CAPABILITY, sslContext.asString(), SSLContext.class),
                        SSLContext.class, service.sslContext);
            }
            if(securityRealm.isDefined()) {
                SecurityRealm.ServiceUtil.addDependency(builder, service.securityRealm, securityRealm.asString(), false);
            }
            builder.install();

        }
    }

    private static final class ReverseProxyHostService implements Service<ReverseProxyHostService> {

        private final InjectedValue<HttpHandler> proxyHandler = new InjectedValue<>();
        private final InjectedValue<OutboundSocketBinding> socketBinding = new InjectedValue<>();
        private final InjectedValue<SecurityRealm> securityRealm = new InjectedValue<>();
        private final InjectedValue<SSLContext> sslContext = new InjectedValue<>();

        private final String instanceId;
        private final String scheme;
        private final String path;

        private ReverseProxyHostService(String scheme, String instanceId, String path) {
            this.instanceId = instanceId;
            this.scheme = scheme;
            this.path = path;
        }
        private URI getUri() throws URISyntaxException {
            OutboundSocketBinding binding = socketBinding.getValue();
            return new URI(scheme, null, binding.getUnresolvedDestinationAddress(), binding.getDestinationPort(), path, null, null);
        }

        @Override
        public void start(StartContext startContext) throws StartException {
            //todo: this is a bit of a hack, as the proxy handler may be wrapped by a request controller handler for graceful shutdown
            ProxyHandler proxyHandler = (ProxyHandler) (this.proxyHandler.getValue() instanceof GlobalRequestControllerHandler ? ((GlobalRequestControllerHandler)this.proxyHandler.getValue()).getNext() : this.proxyHandler.getValue());

            final LoadBalancingProxyClient client = (LoadBalancingProxyClient) proxyHandler.getProxyClient();
            try {
                SSLContext sslContext = this.sslContext.getOptionalValue();
                if (sslContext == null) {
                    SecurityRealm securityRealm = this.securityRealm.getOptionalValue();
                    if (securityRealm != null) {
                        sslContext = securityRealm.getSSLContext();
                    }
                }

                if (sslContext == null) {
                    client.addHost(getUri(), instanceId);
                } else {
                    OptionMap.Builder builder = OptionMap.builder();
                    builder.set(Options.USE_DIRECT_BUFFERS, true);
                    OptionMap combined = builder.getMap();

                    XnioSsl xnioSsl = new UndertowXnioSsl(Xnio.getInstance(), combined, sslContext);
                    client.addHost(getUri(), instanceId, xnioSsl);
                }
            } catch (URISyntaxException e) {
                throw new StartException(e);
            }
        }

        @Override
        public void stop(StopContext stopContext) {
            ProxyHandler proxyHandler = (ProxyHandler) (this.proxyHandler.getValue() instanceof GlobalRequestControllerHandler ? ((GlobalRequestControllerHandler)this.proxyHandler.getValue()).getNext() : this.proxyHandler.getValue());
            final LoadBalancingProxyClient client = (LoadBalancingProxyClient) proxyHandler.getProxyClient();
            try {
                client.removeHost(getUri());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e); //impossible
            }
        }

        @Override
        public ReverseProxyHostService getValue() throws IllegalStateException, IllegalArgumentException {
            return this;
        }
    }

}
