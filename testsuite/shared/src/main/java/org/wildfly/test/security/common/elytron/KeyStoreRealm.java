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


import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron key-store-realm configuration implementation.
 *
 * @author Ondrej Kotek
 */
public class KeyStoreRealm extends AbstractConfigurableElement {

    private final String keyStore;

    private KeyStoreRealm(Builder builder) {
        super(builder);
        this.keyStore = Objects.requireNonNull(builder.keyStore, "Key-store name has to be provided");
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/key-store-realm=%s:add(key-store=\"%s\")", name, keyStore));
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/key-store-realm=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link KeyStoreRealm}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link KeyStoreRealm}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String keyStore;

        private Builder() {
        }

        public Builder withKeyStore(String keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        public KeyStoreRealm build() {
            return new KeyStoreRealm(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
