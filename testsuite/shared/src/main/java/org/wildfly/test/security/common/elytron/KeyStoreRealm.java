/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
