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

import java.util.Objects;

import javax.net.ssl.KeyManagerFactory;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron trust-managers configuration implementation.
 *
 * @author Josef Cacek
 */
public class SimpleTrustManager extends AbstractConfigurableElement implements TrustManager {

    private final String keyStore;

    private SimpleTrustManager(Builder builder) {
        super(builder);
        this.keyStore = Objects.requireNonNull(builder.keyStore, "Key-store name has to be provided");
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/trust-manager=twoWayTM:add(key-store=twoWayTS,algorithm="SunX509")

        cli.sendLine(String.format("/subsystem=elytron/trust-manager=%s:add(key-store=\"%s\",algorithm=\"%s\")", name,
                keyStore, KeyManagerFactory.getDefaultAlgorithm()));
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/trust-manager=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link SimpleTrustManager}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleTrustManager}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String keyStore;

        private Builder() {
        }

        public Builder withKeyStore(String keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        public SimpleTrustManager build() {
            return new SimpleTrustManager(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
