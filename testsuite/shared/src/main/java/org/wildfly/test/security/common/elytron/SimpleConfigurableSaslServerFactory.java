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

package org.wildfly.test.security.common.elytron;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * Elytron configurable-sasl-server-factory configuration.
 *
 * @author Josef Cacek
 */
public class SimpleConfigurableSaslServerFactory extends AbstractConfigurableElement implements SaslServerFactory {

    private final List<SaslFilter> filters;
    private final Map<String, String> properties;
    private final String protocol;
    private final String saslServerFactory;
    private final String serverName;

    private SimpleConfigurableSaslServerFactory(Builder builder) {
        super(builder);
        this.filters = builder.filters;
        this.properties = builder.properties;
        this.protocol = builder.protocol;
        this.saslServerFactory = Objects.requireNonNull(builder.saslServerFactory, "saslServerFactory must not be null");
        this.serverName = builder.serverName;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode op = Util.createAddOperation(
                PathAddress.pathAddress().append("subsystem", "elytron").append("configurable-sasl-server-factory", name));
        if (!filters.isEmpty()) {
            ModelNode filtersNode = op.get("filters");
            for (SaslFilter saslFilter : filters) {
                ModelNode saslFilterNode = new ModelNode();
                setIfNotNull(saslFilterNode, "predefined-filter", saslFilter.getPredefinedFilter());
                setIfNotNull(saslFilterNode, "pattern-filter", saslFilter.getPatternFilter());
                setIfNotNull(saslFilterNode, "enabling", saslFilter.isEnabling());
                filtersNode.add(saslFilterNode);
            }
        }
        if (!properties.isEmpty()) {
            ModelNode propertiesNode = op.get("properties");
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                propertiesNode.get(entry.getKey()).set(entry.getValue());
            }
        }
        setIfNotNull(op, "protocol", protocol);
        op.get("sasl-server-factory").set(saslServerFactory);
        setIfNotNull(op, "server-name", serverName);
        Utils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(
                PathAddress.pathAddress().append("subsystem", "elytron").append("configurable-sasl-server-factory", name)),
                client);
    }

    /**
     * Creates builder to build {@link SimpleConfigurableSaslServerFactory}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleConfigurableSaslServerFactory}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private List<SaslFilter> filters = new ArrayList<SaslFilter>();
        private Map<String, String> properties = new HashMap<>();
        private String protocol;
        private String saslServerFactory;
        private String serverName;

        private Builder() {
        }

        public Builder addFilter(SaslFilter filter) {
            this.filters.add(filter);
            return this;
        }

        public Builder addProperty(String name, String value) {
            this.properties.put(name, value);
            return this;
        }

        public Builder withProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder withSaslServerFactory(String saslServerFactory) {
            this.saslServerFactory = saslServerFactory;
            return this;
        }

        public Builder withServerName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        public SimpleConfigurableSaslServerFactory build() {
            return new SimpleConfigurableSaslServerFactory(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
