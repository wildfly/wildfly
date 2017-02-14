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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
