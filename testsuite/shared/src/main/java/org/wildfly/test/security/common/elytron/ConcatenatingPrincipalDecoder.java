/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
