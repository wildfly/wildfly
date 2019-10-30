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

import java.util.Objects;

import org.jboss.dmr.ModelNode;

/**
 * Single item represantation of client-mappings list attribute in socket-binding configuration.
 *
 * @author Josef Cacek
 */
public class ClientMapping {

    private final String sourceNetwork;
    private final String destinationAddress;
    private final Integer destinationPort;

    private ClientMapping(Builder builder) {
        this.sourceNetwork = builder.sourceNetwork;
        this.destinationAddress = Objects.requireNonNull(builder.destinationAddress, "Destination address has to be provided");
        this.destinationPort = builder.destinationPort;
    }

    public String getSourceNetwork() {
        return sourceNetwork;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public Integer getDestinationPort() {
        return destinationPort;
    }


    public ModelNode toModelNode() {
        final ModelNode node= new ModelNode();
        setIfNotNull(node, "source-network", sourceNetwork);
        setIfNotNull(node, "destination-address", destinationAddress);
        setIfNotNull(node, "destination-port", destinationPort);
        return node;
    }
    /**
     * Creates builder to build {@link ClientMapping}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link ClientMapping}.
     */
    public static final class Builder {
        private String sourceNetwork;
        private String destinationAddress;
        private Integer destinationPort;

        private Builder() {
        }

        public Builder withSourceNetwork(String sourceNetwork) {
            this.sourceNetwork = sourceNetwork;
            return this;
        }

        public Builder withDestinationAddress(String destinationAddress) {
            this.destinationAddress = destinationAddress;
            return this;
        }

        public Builder withDestinationPort(Integer destinationPort) {
            this.destinationPort = destinationPort;
            return this;
        }

        public ClientMapping build() {
            return new ClientMapping(this);
        }
    }

}
