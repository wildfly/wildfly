/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.jboss.dmr.ModelNode;

/**
 * Helper class for adding "credential-reference" attributes into CLI commands.
 *
 * @author Josef Cacek
 */
public class CredentialReference implements CliFragment {

    public static final CredentialReference EMPTY = CredentialReference.builder().build();

    private final String store;
    private final String alias;
    private final String clearText;

    private CredentialReference(Builder builder) {
        this.store = builder.store;
        this.alias = builder.alias;
        this.clearText = builder.clearText;
    }

    @Override
    public String asString() {
        StringBuilder sb = new StringBuilder();
        if (isNotBlank(alias) || isNotBlank(clearText) || isNotBlank(store)) {
            sb.append("credential-reference={ ");
            if (isNotBlank(alias)) {
                sb.append(String.format("alias=\"%s\", ", alias));
            }
            if (isNotBlank(store)) {
                sb.append(String.format("store=\"%s\", ", store));
            }
            if (isNotBlank(clearText)) {
                sb.append(String.format("clear-text=\"%s\"", clearText));
            }
            sb.append("}, ");
        }
        return sb.toString();
    }

    public ModelNode asModelNode() {
        ModelNode credentialReference = new ModelNode();
        if (alias != null) {
            credentialReference.get("alias").set(alias);
        }
        if (store != null) {
            credentialReference.get("store").set(store);
        }
        if (clearText != null) {
            credentialReference.get("clear-text").set(clearText);
        }
        return credentialReference.asObject();
    }

    /**
     * Creates builder to build {@link CredentialReference}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link CredentialReference}.
     */
    public static final class Builder {
        private String store;
        private String alias;
        private String clearText;

        private Builder() {
        }

        public Builder withStore(String store) {
            this.store = store;
            return this;
        }

        public Builder withAlias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder withClearText(String clearText) {
            this.clearText = clearText;
            return this;
        }

        public CredentialReference build() {
            return new CredentialReference(this);
        }
    }

}
