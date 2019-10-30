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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.elytron.AbstractConfigurableElement;

/**
 * Configuration for /subsystem=remoting/connector=*.
 *
 * @author Josef Cacek
 */
public class SimpleRemotingConnector extends AbstractConfigurableElement {

    private final String authenticationProvider;
    private final String saslAuthenticationFactory;
    private final String saslProtocol;
    private final String securityRealm;
    private final String serverName;
    private final String socketBinding;
    private final String sslContext;

    private SimpleRemotingConnector(Builder builder) {
        super(builder);
        this.authenticationProvider = builder.authenticationProvider;
        this.saslAuthenticationFactory = builder.saslAuthenticationFactory;
        this.saslProtocol = builder.saslProtocol;
        this.securityRealm = builder.securityRealm;
        this.serverName = builder.serverName;
        this.socketBinding = builder.socketBinding;
        this.sslContext = builder.sslContext;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode op = Util
                .createAddOperation(PathAddress.pathAddress().append("subsystem", "remoting").append("connector", name));
        setIfNotNull(op, "authentication-provider", authenticationProvider);
        setIfNotNull(op, "sasl-authentication-factory", saslAuthenticationFactory);
        setIfNotNull(op, "sasl-protocol", saslProtocol);
        setIfNotNull(op, "security-realm", securityRealm);
        setIfNotNull(op, "server-name", serverName);
        setIfNotNull(op, "socket-binding", socketBinding);
        setIfNotNull(op, "ssl-context", sslContext);

        Utils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(
                Util.createRemoveOperation(PathAddress.pathAddress().append("subsystem", "remoting").append("connector", name)),
                client);
    }

    /**
     * Creates builder to build {@link SimpleRemotingConnector}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleRemotingConnector}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String authenticationProvider;
        private String saslAuthenticationFactory;
        private String saslProtocol;
        private String securityRealm;
        private String serverName;
        private String socketBinding;
        private String sslContext;

        private Builder() {
        }

        public Builder withAuthenticationProvider(String authenticationProvider) {
            this.authenticationProvider = authenticationProvider;
            return this;
        }

        public Builder withSaslAuthenticationFactory(String saslAuthenticationFactory) {
            this.saslAuthenticationFactory = saslAuthenticationFactory;
            return this;
        }

        public Builder withSaslProtocol(String saslProtocol) {
            this.saslProtocol = saslProtocol;
            return this;
        }

        public Builder withSecurityRealm(String securityRealm) {
            this.securityRealm = securityRealm;
            return this;
        }

        public Builder withServerName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        public Builder withSocketBinding(String socketBinding) {
            this.socketBinding = socketBinding;
            return this;
        }

        public Builder withSslContext(String sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public SimpleRemotingConnector build() {
            return new SimpleRemotingConnector(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
