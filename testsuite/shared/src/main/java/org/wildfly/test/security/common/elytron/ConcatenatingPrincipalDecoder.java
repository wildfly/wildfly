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
import java.util.List;
import java.util.Objects;
import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron concatenating-principal-decoder configuration implementation.
 *
 * @author Ondrej Kotek
 */
public class ConcatenatingPrincipalDecoder extends AbstractConfigurableElement {

    private final String joiner;
    private final List<String> decoders;

    private ConcatenatingPrincipalDecoder(Builder builder) {
        super(builder);
        this.joiner = Objects.toString(builder.joiner, ".");
        this.decoders = Objects.requireNonNull(builder.decoders, "Principal decoders has to be provided");
        if (this.decoders.size() < 2) {
            throw new IllegalArgumentException("At least 2 principal decoders have to be provided");
        }
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/concatenating-principal-decoder=test:add(joiner=".",principal-decoders=[decA, decB]))
        cli.sendLine(String.format("/subsystem=elytron/concatenating-principal-decoder=%s:add(joiner=\"%s\",principal-decoders=[%s])",
                name, joiner, String.join(",", decoders)));
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/concatenating-principal-decoder=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link ConcatenatingPrincipalDecoder}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link ConcatenatingPrincipalDecoder}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String joiner;
        private List<String> decoders;

        private Builder() {
        }

        public Builder withJoiner(String joiner) {
            this.joiner = joiner;
            return this;
        }

        public Builder withDecoders(String... decoders) {
            this.decoders = Arrays.asList(decoders);
            return this;
        }

        public ConcatenatingPrincipalDecoder build() {
            return new ConcatenatingPrincipalDecoder(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
