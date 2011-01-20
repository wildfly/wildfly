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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.remoting.CommonAttributes.AUTHENTICATION_PROVIDER;
import static org.jboss.as.remoting.CommonAttributes.FORWARD_SECRECY;
import static org.jboss.as.remoting.CommonAttributes.INCLUDE_MECHANISMS;
import static org.jboss.as.remoting.CommonAttributes.NO_ACTIVE;
import static org.jboss.as.remoting.CommonAttributes.NO_ANONYMOUS;
import static org.jboss.as.remoting.CommonAttributes.NO_DICTIONARY;
import static org.jboss.as.remoting.CommonAttributes.NO_PLAINTEXT;
import static org.jboss.as.remoting.CommonAttributes.PASS_CREDENTIALS;
import static org.jboss.as.remoting.CommonAttributes.POLICY;
import static org.jboss.as.remoting.CommonAttributes.QOP;
import static org.jboss.as.remoting.CommonAttributes.SASL;
import static org.jboss.as.remoting.CommonAttributes.SERVER_AUTH;
import static org.jboss.as.remoting.CommonAttributes.SOCKET_BINDING;
import static org.jboss.as.remoting.CommonAttributes.STRENGTH;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.xnio.OptionMap;
import org.jboss.xnio.Options;
import org.jboss.xnio.SaslQop;
import org.jboss.xnio.SaslStrength;
import org.jboss.xnio.Sequence;

/**
 * @author Emanuel Muckenhuber
 */
public class NewConnectorAdd implements RuntimeOperationHandler, ModelAddOperationHandler {

    static final OperationHandler INSTANCE = new NewConnectorAdd();

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode address = operation.get(OP_ADDR);
        final String name = address.get(address.asInt() - 1).asString();

        // Create the service.
        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceTarget target = runtimeContext.getServiceTarget();

            final ConnectorService connectorService = new ConnectorService();
            connectorService.setOptionMap(createOptionMap(operation));

            // Register the service with the container and inject dependencies.
            final ServiceName connectorName = ConnectorElement.connectorName(name);
            try {
                target.addService(connectorName, connectorService)
                    .addDependency(connectorName.append("auth-provider"), ServerAuthenticationProvider.class, connectorService.getAuthenticationProviderInjector())
                    .addDependency(RemotingSubsystemElement.JBOSS_REMOTING_ENDPOINT, Endpoint.class, connectorService.getEndpointInjector())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

            // TODO create XNIO connector service from socket-binding, with dependency on connectorName
            } catch (ServiceRegistryException e) {
                resultHandler.handleFailed(null);
            }
        }

        // Compensating is remove
        final ModelNode compensating = new ModelNode();
        compensating.get(OP_ADDR).set(address);
        compensating.get(OP).set(REMOVE);
        compensating.get(NAME).set(name);

        // Apply to model
        applyToModel(context.getSubModel(), operation);


        resultHandler.handleResultComplete(compensating);

        return Cancellable.NULL;
    }

    static void applyToModel(final ModelNode subModel, final ModelNode parameters) {

        subModel.get(SOCKET_BINDING).set(parameters.require(SOCKET_BINDING));
        if(parameters.has(AUTHENTICATION_PROVIDER)) {
            subModel.get(AUTHENTICATION_PROVIDER).set(parameters.get(AUTHENTICATION_PROVIDER));
        }
        if(parameters.has(SASL)) {
            subModel.get(SASL).set(parameters.get(SASL));
        }
    }

    static OptionMap createOptionMap(final ModelNode parameters) {
        final OptionMap.Builder builder = OptionMap.builder();

        if(parameters.has(SASL)) {
            final ModelNode sasl = parameters.require(SASL);
            builder.set(Options.SASL_SERVER_AUTH, sasl.get(SERVER_AUTH).asBoolean());
            builder.set(Options.SASL_STRENGTH, SaslStrength.valueOf(sasl.get(STRENGTH).asString()));
            builder.set(Options.SASL_QOP, Sequence.of(asQopSet(sasl.get(QOP))));
            builder.set(Options.SASL_MECHANISMS, Sequence.of(asStringSet(sasl.get(INCLUDE_MECHANISMS))));

            if(sasl.has(POLICY)) {
                final ModelNode policy = sasl.require(POLICY);
                builder.set(Options.SASL_POLICY_FORWARD_SECRECY, policy.get(FORWARD_SECRECY).asBoolean());
                builder.set(Options.SASL_POLICY_NOACTIVE, policy.get(NO_ACTIVE).asBoolean());
                builder.set(Options.SASL_POLICY_NOANONYMOUS, policy.get(NO_ANONYMOUS).asBoolean());
                builder.set(Options.SASL_POLICY_NODICTIONARY, policy.get(NO_DICTIONARY).asBoolean());
                builder.set(Options.SASL_POLICY_NOPLAINTEXT, policy.get(NO_PLAINTEXT).asBoolean());
                builder.set(Options.SASL_POLICY_PASS_CREDENTIALS, policy.get(PASS_CREDENTIALS).asBoolean());
            }
        }
        return builder.getMap();
    }

    static Collection<String> asStringSet(final ModelNode node) {
        final Set<String> set = new HashSet<String>();
        for(final ModelNode element : node.asList()) {
            set.add(element.asString());
        }
        return set;
    }

    static Collection<SaslQop> asQopSet(final ModelNode node) {
        final Set<SaslQop> set = new HashSet<SaslQop>();
        for(final ModelNode element : node.asList()) {
            set.add(SaslQop.valueOf(element.asString()));
        }
        return set;
    }

}
