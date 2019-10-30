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

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Updates Undertow https-listener of the defaul-server to use given (Elytron server-ssl-context) SSL context
 * instead of SSL context from legacy security-realm.
 *
 * @author Ondrej Kotek
 */
public class UndertowSslContext extends AbstractConfigurableElement {

    private String httpsListener;

    private UndertowSslContext(Builder builder) {
        super(builder);
        this.httpsListener = builder.httpsListener;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        cli.sendLine("batch");
        cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:undefine-attribute" +
                "(name=security-realm)", httpsListener));
        cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:write-attribute" +
                "(name=ssl-context,value=%s)", httpsListener, name));
        cli.sendLine("run-batch");
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine("batch");
        cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:undefine-attribute" +
                "(name=ssl-context)", httpsListener));
        cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:write-attribute" +
                "(name=security-realm,value=ApplicationRealm)", httpsListener));
        cli.sendLine("run-batch");
    }

    /**
     * Creates builder to build {@link UndertowSslContext}. The name attribute refers to ssl-context capability name.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link UndertowSslContext}. The name attribute refers to ssl-context capability name.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String httpsListener = "https";

        private Builder() {
        }

        public Builder withHttpsListener(String httpsListener) {
            this.httpsListener = httpsListener;
            return this;
        }

        public UndertowSslContext build() {
            return new UndertowSslContext(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
