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
package org.wildfly.test.undertow.common.elytron;

import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.wildfly.test.security.common.elytron.AbstractConfigurableElement;

/**
 * Undertow https-listener configuration implementation.
 *
 * @author Jan Stourac
 */
public class SimpleHttpsListener extends AbstractConfigurableElement implements HttpsListener {

    private final String socketBinding;
    private final String sslContext;
    private final String securityRealm;

    private SimpleHttpsListener(Builder builder) {
        super(builder);
        this.socketBinding = builder.socketBinding;
        this.sslContext = builder.sslContext;
        this.securityRealm = builder.securityRealm;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        if (sslContext != null) {
            // /subsystem=undertow/server=default-server/https-listener=https-test:add(socket-binding=https-test,
            // ssl-context=sslContext)
            cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:add" +
                    "(socket-binding=%s,ssl-context=%s)", name, socketBinding, sslContext));
        } else {
            // /subsystem=undertow/server=default-server/https-listener=https-test:add(socket-binding=https-test,
            // security-realm=secRealm)
            cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:add" +
                    "(socket-binding=%s,security-realm=%s)", name, socketBinding, securityRealm));
        }
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link SimpleHttpsListener}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleHttpsListener}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String socketBinding;
        private String sslContext;
        private String securityRealm;

        private Builder() {
        }

        public Builder withSocketBinding(String socketBinding) {
            this.socketBinding = socketBinding;
            return this;
        }

        public Builder withSslContext(String sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder withSecurityRealm(String securityRealm) {
            this.securityRealm = securityRealm;
            return this;
        }

        public SimpleHttpsListener build() {
            if (socketBinding == null) {
                throw new RuntimeException("socket-binding is required when creating https-listener");
            }

            if ((sslContext == null && securityRealm == null) || (sslContext != null && securityRealm != null)) {
                throw new RuntimeException("exactly one of ssl-context and security-realm must be defined when " +
                        "creating https-listener");
            }

            return new SimpleHttpsListener(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
