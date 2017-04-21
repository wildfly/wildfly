/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.security.common.other;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.elytron.AbstractConfigurableElement;

/**
 * Socket binding configuration. The socketBindingGroup has to exist - if this attribute is not provided, then it defaults to
 * "standard-sockets" value.
 *
 * @author Josef Cacek
 */
public class SimpleSocketBinding extends AbstractConfigurableElement {

    private final String socketBindingGroup;
    private final List<ClientMapping> clientMappings;
    private final Boolean fixedPort;
    private final String interfaceName;
    private final String multicastAddress;
    private final Integer multicastPort;
    private final Integer port;

    private SimpleSocketBinding(Builder builder) {
        super(builder);
        this.socketBindingGroup = builder.socketBindingGroup != null ? builder.socketBindingGroup : "standard-sockets";
        this.clientMappings = new ArrayList<>(builder.clientMappings);
        this.fixedPort = builder.fixedPort;
        this.interfaceName = builder.interfaceName;
        this.multicastAddress = builder.multicastAddress;
        this.multicastPort = builder.multicastPort;
        this.port = builder.port;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode op = Util.createAddOperation(
                PathAddress.pathAddress().append("socket-binding-group", socketBindingGroup).append("socket-binding", name));
        if (!clientMappings.isEmpty()) {
            ModelNode mappings = op.get("client-mappings");
            for (ClientMapping mapping : clientMappings) {
                mappings.add(mapping.toModelNode());
            }
        }

        setIfNotNull(op, "fixed-port", fixedPort);
        setIfNotNull(op, "interface", interfaceName);
        setIfNotNull(op, "multicast-address", multicastAddress);
        setIfNotNull(op, "multicast-port", multicastPort);
        setIfNotNull(op, "port", port);

        Utils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(
                PathAddress.pathAddress().append("socket-binding-group", socketBindingGroup).append("socket-binding", name)),
                client);
    }

    /**
     * Creates builder to build {@link SimpleSocketBinding}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleSocketBinding}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String socketBindingGroup;
        private List<ClientMapping> clientMappings = new ArrayList<>();
        private Boolean fixedPort;
        private String interfaceName;
        private String multicastAddress;
        private Integer multicastPort;
        private Integer port;

        private Builder() {
        }

        public Builder withSocketBindingGroup(String socketBindingGroup) {
            this.socketBindingGroup = socketBindingGroup;
            return this;
        }

        public Builder addClientMapping(ClientMapping clientMapping) {
            this.clientMappings.add(clientMapping);
            return this;
        }

        public Builder withFixedPort(Boolean fixedPort) {
            this.fixedPort = fixedPort;
            return this;
        }

        public Builder withInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
            return this;
        }

        public Builder withMulticastAddress(String multicastAddress) {
            this.multicastAddress = multicastAddress;
            return this;
        }

        public Builder withMulticastPort(Integer multicastPort) {
            this.multicastPort = multicastPort;
            return this;
        }

        public Builder withPort(Integer port) {
            this.port = port;
            return this;
        }

        public SimpleSocketBinding build() {
            return new SimpleSocketBinding(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
