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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron server-ssl-context configuration implementation.
 *
 * @author Josef Cacek
 */
public class SimpleServerSslContext extends AbstractConfigurableElement implements ServerSslContext {

    private final String keyManagers;
    private final String trustManagers;
    private final String[] protocols;
    private final boolean needClientAuth;

    private SimpleServerSslContext(Builder builder) {
        super(builder);
        this.keyManagers = builder.keyManagers;
        this.trustManagers = builder.trustManagers;
        this.protocols = builder.protocols;
        this.needClientAuth = builder.needClientAuth;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/server-ssl-context=twoWaySSC:add(key-managers=twoWayKM,protocols=["TLSv1.2"],trust-managers=twoWayTM,need-client-auth=true)
        StringBuilder sb = new StringBuilder("/subsystem=elytron/server-ssl-context=").append(name).append(":add(");
        if (StringUtils.isNotBlank(keyManagers)) {
            sb.append("key-managers=\"").append(keyManagers).append("\", ");
        }
        if (protocols != null) {
            sb.append("protocols=[")
                    .append(Arrays.stream(protocols).map(s -> "\"" + s + "\"").collect(Collectors.joining(", "))).append("], ");
        }
        if (StringUtils.isNotBlank(trustManagers)) {
            sb.append("trust-managers=\"").append(trustManagers).append("\", ");
        }
        sb.append("need-client-auth=").append(needClientAuth).append(")");
        cli.sendLine(sb.toString());
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/server-ssl-context=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link SimpleServerSslContext}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleServerSslContext}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String keyManagers;
        private String trustManagers;
        private String[] protocols;
        private boolean needClientAuth;

        private Builder() {
        }

        public Builder withKeyManagers(String keyManagers) {
            this.keyManagers = keyManagers;
            return this;
        }

        public Builder withTrustManagers(String trustManagers) {
            this.trustManagers = trustManagers;
            return this;
        }

        public Builder withProtocols(String... protocols) {
            this.protocols = protocols;
            return this;
        }

        public Builder withNeedClientAuth(boolean needClientAuth) {
            this.needClientAuth = needClientAuth;
            return this;
        }

        public SimpleServerSslContext build() {
            return new SimpleServerSslContext(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
