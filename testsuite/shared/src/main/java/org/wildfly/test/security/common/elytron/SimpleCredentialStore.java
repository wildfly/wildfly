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
import static org.jboss.as.test.shared.CliUtils.escapePath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron credential store (keystore-based) configuration implementation.
 *
 * @author Josef Cacek
 */
public class SimpleCredentialStore extends AbstractConfigurableElement implements CredentialStore {

    // Let's use the Path type for keystore location even the CLI fragment functionality is not used here.
    private final Path keyStorePath;
    private final CredentialReference credential;
    private final String keyStoreType;
    private final Boolean create;
    private final Boolean modifiable;
    private final Map<String, String> aliases;

    private SimpleCredentialStore(Builder builder) {
        super(builder);
        this.keyStorePath = Objects.requireNonNull(builder.keyStorePath, "KeyStore path has to be provided");
        this.credential = builder.credential;
        this.keyStoreType = builder.keyStoreType;
        this.create = builder.create;
        this.modifiable = builder.modifiable;
        this.aliases = Collections.unmodifiableMap(new HashMap<>(builder.aliases));
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/credential-store=test:add(location=a,create=true,modifiable=true,implementation-properties={"keyStoreType"=>"JCEKS"},
        // credential-reference={clear-text=pass123})

        final StringBuilder sb = new StringBuilder("/subsystem=elytron/credential-store=");
        sb.append(name).append(":add(").append("location=")
                .append(escapePath(keyStorePath.getPath()));
        if (create != null) {
            sb.append(",").append("create=").append(create.toString());
        }
        if (modifiable != null) {
            sb.append(",").append("modifiable=").append(modifiable.toString());
        }

        if (keyStoreType != null) {
            sb.append(",")
                    .append("implementation-properties={")
                    .append("\"keyStoreType\"=>\"")
                    .append(keyStoreType)
                    .append("\"}");
        }


        if (credential != null) {
            sb.append(",").append(credential.asString());
        }

        if (isNotBlank(keyStorePath.getRelativeTo())) {
            sb.append(",").append("relative-to=\"").append(keyStorePath.getRelativeTo()).append("\"");
        }
        sb.append(")");
        cli.sendLine(sb.toString());

        for (Entry<String, String> entry : aliases.entrySet()) {
            // /subsystem=elytron/credential-store=test/alias=alias1:add(secret-value=mySecretValue)
            cli.sendLine(String.format("/subsystem=elytron/credential-store=%s:add-alias(alias=%s, secret-value=\"%s\")", name,
                    entry.getKey(), entry.getValue()));
        }
    }

    /**
     * @see org.wildfly.test.security.common.elytron.ConfigurableElement#remove(org.jboss.as.test.integration.management.util.CLIWrapper)
     */
    @Override
    public void remove(CLIWrapper cli) throws Exception {
        // remove aliases
        for (String alias : aliases.keySet()) {
            // lowercase alias used - https://issues.jboss.org/browse/WFLY-8131
            cli.sendLine(String.format("/subsystem=elytron/credential-store=%s:remove-alias(alias=%s)", name, alias.toLowerCase(Locale.ROOT)));
        }

        cli.sendLine(String.format("/subsystem=elytron/credential-store=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link SimpleCredentialStore}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleCredentialStore}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private Path keyStorePath;
        private CredentialReference credential;
        private String keyStoreType;
        private Boolean create;
        private Boolean modifiable;
        private Map<String, String> aliases = new HashMap<>();

        private Builder() {
        }

        public Builder withKeyStorePath(Path keyStorePath) {
            this.keyStorePath = keyStorePath;
            return this;
        }

        public Builder withCredential(CredentialReference credential) {
            this.credential = credential;
            return this;
        }

        public Builder withKeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
            return this;
        }

        public Builder withCreate(Boolean create) {
            this.create = create;
            return this;
        }

        public Builder withModifiable(Boolean modifiable) {
            this.modifiable = modifiable;
            return this;
        }

        /**
         * Adds a named secret (alias + secret value) to the map of aliases to be created in the credential store.
         *
         * @param alias alias for the secret
         * @param secret secret value
         * @return
         */
        public Builder withAlias(String alias, String secret) {
            this.aliases.put(alias, secret);
            return this;
        }

        /**
         * Clears secrets map (aliases).
         *
         * @see #withAlias(String, String)
         */
        public Builder clearAliases() {
            this.aliases.clear();
            return this;
        }

        public SimpleCredentialStore build() {
            return new SimpleCredentialStore(this);
        }

        @Override
        protected Builder self() {
            return this;
        }

    }

}
