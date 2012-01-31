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

package org.jboss.as.remoting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.remoting.CommonAttributes.SECURITY_REALM;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.xnio.OptionMap;

/**
 * Add a connector to a remoting container.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class ConnectorAdd extends AbstractAddStepHandler {

    static final ConnectorAdd INSTANCE = new ConnectorAdd();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException{
        ConnectorResource.SOCKET_BINDING.validateAndSet(operation, model);
        ConnectorResource.AUTHENTICATION_PROVIDER.validateAndSet(operation, model);
        if (operation.hasDefined(SECURITY_REALM)) {
            model.get(SECURITY_REALM).set(operation.get(SECURITY_REALM).asString());
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String connectorName = address.getLastElement().getValue();
        ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "jboss.controller.temp.dir");
        final ServiceName securityRealm = model.hasDefined(SECURITY_REALM) ? SecurityRealmService.BASE_SERVICE_NAME
                .append(model.require(SECURITY_REALM).asString()) : null;
        RemotingServices.installSecurityServices(context.getServiceTarget(), connectorName, securityRealm, null, tmpDirPath, verificationHandler, newControllers);
        launchServices(context, address, connectorName, model, verificationHandler, newControllers);
    }

    void launchServices(OperationContext context, PathAddress pathAddress, String connectorName, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        //TODO SASL and properties

        final ServiceTarget target = context.getServiceTarget();

        final ServiceName socketBindingName = SocketBinding.JBOSS_BINDING_NAME.append(ConnectorResource.SOCKET_BINDING.resolveModelAttribute(context, model).asString());
        final OptionMap optionMap = ConnectorResource.getOptions(context, pathAddress);
        RemotingServices.installConnectorServicesForSocketBinding(target, RemotingServices.SUBSYSTEM_ENDPOINT, connectorName, socketBindingName, optionMap, verificationHandler, newControllers);

        //TODO AuthenticationHandler
//
//        final ConnectorService connectorService = new ConnectorService();
//        connectorService.setOptionMap(createOptionMap(operation));
//
//        // Register the service with the container and inject dependencies.
//        final ServiceName connectorName = RemotingServices.connectorServiceName(name);
//        try {
//            newControllers.add(target.addService(connectorName, connectorService)
//                    .addDependency(connectorName.append("auth-provider"), ServerAuthenticationProvider.class, connectorService.getAuthenticationProviderInjector())
//                    .addDependency(RemotingServices.SUBSYSTEM_ENDPOINT, Endpoint.class, connectorService.getEndpointInjector())
//                    .addListener(verificationHandler)
//                    .setInitialMode(ServiceController.Mode.ACTIVE)
//                    .install());
//
//            // TODO create XNIO connector service from socket-binding, with dependency on connectorName
//        } catch (ServiceRegistryException e) {
//            throw new OperationFailedException(new ModelNode().set(e.getLocalizedMessage()));
//        }
    }

   /* static OptionMap createOptionMap(final ModelNode parameters) {
        final OptionMap.Builder builder = OptionMap.builder();

        if (parameters.hasDefined(SASL)) {
            final ModelNode sasl = parameters.require(SASL);
            builder.set(Options.SASL_SERVER_AUTH, sasl.get(SERVER_AUTH).asBoolean());
            builder.set(Options.SASL_STRENGTH, SaslStrength.valueOf(sasl.get(STRENGTH).asString()));
            builder.set(Options.SASL_QOP, Sequence.of(asQopSet(sasl.get(QOP))));
            builder.set(Options.SASL_MECHANISMS, Sequence.of(asStringSet(sasl.get(INCLUDE_MECHANISMS))));

            if (sasl.hasDefined(POLICY)) {
                final ModelNode policy = sasl.require(POLICY);
                builder.set(Options.SASL_POLICY_FORWARD_SECRECY, policy.get(FORWARD_SECRECY).asBoolean());
                builder.set(Options.SASL_POLICY_NOACTIVE, policy.get(NO_ACTIVE).asBoolean());
                builder.set(Options.SASL_POLICY_NOANONYMOUS, policy.get(NO_ANONYMOUS).asBoolean());
                builder.set(Options.SASL_POLICY_NODICTIONARY, policy.get(NO_DICTIONARY).asBoolean());
                builder.set(Options.SASL_POLICY_NOPLAINTEXT, policy.get(NO_PLAIN_TEXT).asBoolean());
                builder.set(Options.SASL_POLICY_PASS_CREDENTIALS, policy.get(PASS_CREDENTIALS).asBoolean());
            }
        }
        return builder.getMap();
    }

    static Collection<String> asStringSet(final ModelNode node) {
        final Set<String> set = new HashSet<String>();
        for (final ModelNode element : node.asList()) {
            set.add(element.asString());
        }
        return set;
    }

    static Collection<SaslQop> asQopSet(final ModelNode node) {
        final Set<SaslQop> set = new HashSet<SaslQop>();
        for (final ModelNode element : node.asList()) {
            set.add(SaslQop.valueOf(element.asString()));
        }
        return set;
    }
    */
}
