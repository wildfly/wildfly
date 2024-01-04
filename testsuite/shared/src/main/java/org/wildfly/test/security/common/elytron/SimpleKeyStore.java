/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.common.elytron;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron key-store configuration implementation.
 *
 * @author Josef Cacek
 */
public class SimpleKeyStore extends AbstractConfigurableElement implements KeyStore {

    private final Path path;
    private final CredentialReference credentialReference;
    private final String type;
    private final boolean required;

    private SimpleKeyStore(Builder builder) {
        super(builder);
        this.path = defaultIfNull(builder.path, Path.EMPTY);
        this.credentialReference = defaultIfNull(builder.credentialReference, CredentialReference.EMPTY);
        this.type = defaultIfNull(builder.type, "JKS");
        this.required = builder.required;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/key-store=httpsKS:add(path=keystore.jks,relative-to=jboss.server.config.dir,
        // credential-reference={clear-text=secret},type=JKS,required=false)
        cli.sendLine(String.format("/subsystem=elytron/key-store=%s:add(%s%stype=\"%s\",required=%s)", name, path.asString(),
                credentialReference.asString(), type, Boolean.toString(required)));
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/key-store=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link SimpleKeyStore}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleKeyStore}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private Path path;
        private CredentialReference credentialReference;
        private String type;
        private boolean required;

        private Builder() {
        }

        public Builder withPath(Path path) {
            this.path = path;
            return this;
        }

        public Builder withCredentialReference(CredentialReference credentialReference) {
            this.credentialReference = credentialReference;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withRequired(boolean required) {
            this.required = required;
            return this;
        }

        public SimpleKeyStore build() {
            return new SimpleKeyStore(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
