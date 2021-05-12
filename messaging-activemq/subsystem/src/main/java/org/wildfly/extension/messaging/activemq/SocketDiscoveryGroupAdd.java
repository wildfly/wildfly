/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for adding a discovery group using socket bindings.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class SocketDiscoveryGroupAdd extends AbstractAddStepHandler {

    public static final SocketDiscoveryGroupAdd INSTANCE = new SocketDiscoveryGroupAdd(true);
    public static final SocketDiscoveryGroupAdd LEGACY_INSTANCE = new SocketDiscoveryGroupAdd(false);

    private final boolean needLegacyCall;

    private SocketDiscoveryGroupAdd(boolean needLegacyCall) {
        super(SocketDiscoveryGroupDefinition.ATTRIBUTES);
        this.needLegacyCall = needLegacyCall;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        super.execute(context, operation);
        if(needLegacyCall) {
            PathAddress target = context.getCurrentAddress().getParent().append(CommonAttributes.DISCOVERY_GROUP, context.getCurrentAddressValue());
            ModelNode op = operation.clone();
            op.get(OP_ADDR).set(target.toModelNode());
            context.addStep(op, DiscoveryGroupAdd.LEGACY_INSTANCE, OperationContext.Stage.MODEL, true);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        ServiceRegistry registry = context.getServiceRegistry(false);
        ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = serviceName == null ? null : registry.getService(serviceName);
        if (service != null) {
            context.reloadRequired();
        } else {
            final ServiceTarget target = context.getServiceTarget();
            if (model.hasDefined(JGROUPS_CLUSTER.getName())) {
                // nothing to do, in that case, the clustering.jgroups subsystem will have setup the stack
            } else if(model.hasDefined(RemoteTransportDefinition.SOCKET_BINDING.getName())) {
                if(serviceName == null) {
                    serviceName = MessagingServices.getActiveMQServiceName((String) null);
                }
                ServiceBuilder builder = target.addService(GroupBindingService.getDiscoveryBaseServiceName(serviceName).append(name));
                builder.setInstance(new GroupBindingService(builder.requires(SocketBinding.JBOSS_BINDING_NAME.append(model.get(SOCKET_BINDING).asString()))));
                builder.install();
            }
        }
    }

    static Map<String, DiscoveryGroupConfiguration> addDiscoveryGroupConfigs(final OperationContext context, final ModelNode model)  throws OperationFailedException {
         Map<String, DiscoveryGroupConfiguration> configs = new HashMap<>();
        if (model.hasDefined(CommonAttributes.SOCKET_DISCOVERY_GROUP)) {
            for (Property prop : model.get(CommonAttributes.SOCKET_DISCOVERY_GROUP).asPropertyList()) {
                configs.put(prop.getName(), createDiscoveryGroupConfiguration(context, prop.getName(), prop.getValue()));
            }
        }
        return configs;
    }

    static DiscoveryGroupConfiguration createDiscoveryGroupConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final long refreshTimeout = DiscoveryGroupDefinition.REFRESH_TIMEOUT.resolveModelAttribute(context, model).asLong();
        final long initialWaitTimeout = DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT.resolveModelAttribute(context, model).asLong();

        return new DiscoveryGroupConfiguration()
                .setName(name)
                .setRefreshTimeout(refreshTimeout)
                .setDiscoveryInitialWaitTimeout(initialWaitTimeout);
    }

   public static DiscoveryGroupConfiguration createDiscoveryGroupConfiguration(final String name, final DiscoveryGroupConfiguration config, final SocketBinding socketBinding) throws Exception {

        final String localAddress = socketBinding.getAddress().getHostAddress();
        final String groupAddress = socketBinding.getMulticastAddress().getHostAddress();
        final int groupPort = socketBinding.getMulticastPort();
        final long refreshTimeout = config.getRefreshTimeout();
        final long initialWaitTimeout = config.getDiscoveryInitialWaitTimeout();

        final BroadcastEndpointFactory endpointFactory = new UDPBroadcastEndpointFactory()
                .setGroupAddress(groupAddress)
                .setGroupPort(groupPort)
                .setLocalBindAddress(localAddress)
                .setLocalBindPort(-1);

        return new DiscoveryGroupConfiguration()
                .setName(name)
                .setRefreshTimeout(refreshTimeout)
                .setDiscoveryInitialWaitTimeout(initialWaitTimeout)
                .setBroadcastEndpointFactory(endpointFactory);
    }
}
