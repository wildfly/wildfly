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
