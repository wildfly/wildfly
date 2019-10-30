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
import java.util.List;
import java.util.Objects;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * Elytron sasl-authentication-factory configuration.
 *
 * @author Josef Cacek
 */
public class SimpleSaslAuthenticationFactory extends AbstractConfigurableElement implements SaslAuthenticationFactory {

    private final List<MechanismConfiguration> mechanismConfigurations;
    private final String saslServerFactory;
    private final String securityDomain;


    private SimpleSaslAuthenticationFactory(Builder builder) {
        super(builder);
        this.mechanismConfigurations = new ArrayList<>(builder.mechanismConfigurations);
        this.saslServerFactory = Objects.requireNonNull(builder.saslServerFactory,"saslServerFactory must be not-null");
        this.securityDomain = Objects.requireNonNull(builder.securityDomain,"securityDomain must be not-null");
    }


    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode op = Util.createAddOperation(
                PathAddress.pathAddress().append("subsystem", "elytron").append("sasl-authentication-factory", name));
        setIfNotNull(op, "sasl-server-factory", saslServerFactory);
        setIfNotNull(op, "security-domain", securityDomain);
        if (!mechanismConfigurations.isEmpty()) {
            ModelNode confs = op.get("mechanism-configurations");
            for (MechanismConfiguration conf : mechanismConfigurations) {
                confs.add(conf.toModelNode());
            }
        }
        Utils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(
                PathAddress.pathAddress().append("subsystem", "elytron").append("sasl-authentication-factory", name)),
                client);
    }


    /**
     * Creates builder to build {@link SimpleSaslAuthenticationFactory}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleSaslAuthenticationFactory}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private List<MechanismConfiguration> mechanismConfigurations = new ArrayList<>();
        private String saslServerFactory;
        private String securityDomain;

        private Builder() {
        }

        public Builder addMechanismConfiguration(MechanismConfiguration mechanismConfiguration) {
            this.mechanismConfigurations.add(mechanismConfiguration);
            return this;
        }

        public Builder withSaslServerFactory(String saslServerFactory) {
            this.saslServerFactory = saslServerFactory;
            return this;
        }

        public Builder withSecurityDomain(String securityDomain) {
            this.securityDomain = securityDomain;
            return this;
        }

        public SimpleSaslAuthenticationFactory build() {
            return new SimpleSaslAuthenticationFactory(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
