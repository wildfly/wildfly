/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.UndertowRootDefinition.HTTP_INVOKER_RUNTIME_CAPABILITY;
import static org.wildfly.extension.undertow.UndertowService.CAPABILITY_NAME_HTTP_INVOKER_HOST;

import java.util.Arrays;
import java.util.Collection;

import io.undertow.server.handlers.PathHandler;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilitiesServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;

/**
 * @author Stuart Douglas
 */
public class HttpInvokerDefinition extends PersistentResourceDefinition {

    static final RuntimeCapability<Void> HTTP_INVOKER_HOST_CAPABILITY =
                RuntimeCapability.Builder.of(CAPABILITY_NAME_HTTP_INVOKER_HOST, false, PathHandler.class)
                        .setDynamicNameMapper(address -> new String[]{
                                address.getParent().getLastElement().getValue(),
                                address.getLastElement().getValue()})
                        .build();

    protected static final SimpleAttributeDefinition HTTP_AUTHENTICATION_FACTORY = new SimpleAttributeDefinitionBuilder(Constants.HTTP_AUTHENITCATION_FACTORY, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, true))
            .setRestartAllServices()
            .build();

    protected static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("wildfly-services"))
            .setRestartAllServices()
            .build();

    static final Collection<SimpleAttributeDefinition> ATTRIBUTES = Arrays.asList(
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            PATH,
            HTTP_AUTHENTICATION_FACTORY
    );
    static final HttpInvokerDefinition INSTANCE = new HttpInvokerDefinition();

    private HttpInvokerDefinition() {
        super(new Parameters(UndertowExtension.PATH_HTTP_INVOKER, UndertowExtension.getResolver(Constants.HTTP_INVOKER))
                .setAddHandler(new HttpInvokerAdd())
                .setRemoveHandler(new HttpInvokerRemove())
                .setCapabilities(HTTP_INVOKER_HOST_CAPABILITY)
        );


    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return (Collection) ATTRIBUTES;
    }


    private static final class HttpInvokerAdd extends AbstractAddStepHandler {

        public HttpInvokerAdd() {
            super(ATTRIBUTES);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final PathAddress address = context.getCurrentAddress();
            final PathAddress hostAddress = address.getParent();
            final PathAddress serverAddress = hostAddress.getParent();
            String path = PATH.resolveModelAttribute(context, model).asString();
            String httpAuthenticationFactory = null;
            final ModelNode authFactory = HTTP_AUTHENTICATION_FACTORY.resolveModelAttribute(context, model);
            if (authFactory.isDefined()) {
                httpAuthenticationFactory = authFactory.asString();
            }

            final HttpInvokerHostService service = new HttpInvokerHostService(path);
            final String serverName = serverAddress.getLastElement().getValue();
            final String hostName = hostAddress.getLastElement().getValue();

            final CapabilitiesServiceBuilder<HttpInvokerHostService> builder = context.getServiceTarget()
                    .addCapability(HTTP_INVOKER_HOST_CAPABILITY, service)
                    .addCapabilityRequirement(HTTP_INVOKER_RUNTIME_CAPABILITY.getName(), PathHandler.class, service.getRemoteHttpInvokerServiceInjectedValue())
                    .addCapabilityRequirement(UndertowService.CAPABILITY_NAME_HOST, Host.class, service.getHost(), serverName, hostName)
                    ;

            if (httpAuthenticationFactory != null) {
                builder.addCapabilityRequirement(UndertowService.CAP_REF_HTTP_AUTHENITCATION_FACTORY, HttpAuthenticationFactory.class, service.getHttpAuthenticationFactoryInjectedValue(), httpAuthenticationFactory);
            }

            builder.setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
        }
    }

    private static final class HttpInvokerRemove extends ServiceRemoveStepHandler {

        protected HttpInvokerRemove() {
            super(new HttpInvokerAdd());
        }


        @Override
        protected ServiceName serviceName(String name, PathAddress address) {
            /*final PathAddress hostAddress = address.subAddress(0, address.size() - 1);
            final PathAddress serverAddress = hostAddress.subAddress(0, hostAddress.size() - 1);
            final String serverName = serverAddress.getLastElement().getValue();
            final String hostName = hostAddress.getLastElement().getValue();
            return UndertowService.virtualHostName(serverName, hostName).append(Constants.HTTP_INVOKER);*/
            return HTTP_INVOKER_HOST_CAPABILITY.getCapabilityServiceName(address);
        }
    }
}
