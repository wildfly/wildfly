/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HOST;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
class ConnectorService implements Service {

    private final Supplier<ActiveMQBroker> serverSupplier;
    private final Supplier<SocketBinding> socketBindingSupplier;
    private final Supplier<OutboundSocketBinding> outboundSocketBindingSupplier;
    private final String factoryClass;
    private final Map<String, Object> parameters;
    private final Map<String, Object> extraParameters;
    private final String name;

    ConnectorService(Supplier<ActiveMQBroker> serverSupplier, Supplier<SocketBinding> socketBindingSupplier, Supplier<OutboundSocketBinding> outboundSocketBindingSupplier, String factoryClass, Map<String, Object> parameters, Map<String, Object> extraParameters, String name) {
        this.serverSupplier = serverSupplier;
        this.socketBindingSupplier = socketBindingSupplier;
        this.outboundSocketBindingSupplier = outboundSocketBindingSupplier;
        this.factoryClass = factoryClass;
        this.parameters = parameters;
        this.extraParameters = extraParameters;
        this.name = name;
    }

    @Override
    public void start(StartContext sc) throws StartException {
        if (outboundSocketBindingSupplier != null) {
            OutboundSocketBinding binding = outboundSocketBindingSupplier.get();
            if (binding.getSourceAddress() != null) {
                parameters.put(TransportConstants.LOCAL_ADDRESS_PROP_NAME,
                        NetworkUtils.canonize(binding.getSourceAddress().getHostAddress()));
            }
            if (binding.getSourcePort() != null) {
                // Use absolute port to account for source port offset/fixation
                parameters.put(TransportConstants.LOCAL_PORT_PROP_NAME, binding.getAbsoluteSourcePort());
            }
            parameters.put(HOST, NetworkUtils.canonize(binding.getUnresolvedDestinationAddress()));
            parameters.put(PORT, binding.getDestinationPort());
        } else if (socketBindingSupplier != null) {
            SocketBinding binding = socketBindingSupplier.get();
            if (binding.getClientMappings() != null && !binding.getClientMappings().isEmpty()) {
                // At the moment ActiveMQ doesn't allow selecting mapping based on client's network.
                // Instead the first client-mapping element will always be used - see WFLY-8432
                ClientMapping clientMapping = binding.getClientMappings().get(0);
                parameters.put(HOST, NetworkUtils.canonize(clientMapping.getDestinationAddress()));
                parameters.put(PORT, clientMapping.getDestinationPort());
            } else {
                InetSocketAddress sa = binding.getSocketAddress();
                parameters.put(PORT, sa.getPort());
                // resolve the host name of the address only if a loopback address has been set
                if (sa.getAddress().isLoopbackAddress()) {
                    parameters.put(HOST, NetworkUtils.canonize(sa.getAddress().getHostName()));
                } else {
                    parameters.put(HOST, NetworkUtils.canonize(sa.getAddress().getHostAddress()));
                }
            }
        }
        try {
            serverSupplier.get().addConnectorConfiguration(name, new TransportConfiguration(factoryClass, parameters, name, extraParameters));
        } catch (Exception ex) {
            throw new StartException(ex);
        }
    }

    @Override
    public void stop(StopContext sc) {
    }
}
