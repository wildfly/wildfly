/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.common.elytron;

/**
 * Elytron constant-principal-decoder configuration implementation.
 *
 * @author Ondrej Kotek
 */
public class ConstantPrincipalDecoder extends AbstractConstantHelper {

    private ConstantPrincipalDecoder(Builder builder) {
        super(builder);
    }


    @Override
    protected String getConstantElytronType() {
        return "constant-principal-decoder";
    }

    /**
     * Creates builder.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for the class.
     */
    public static final class Builder extends AbstractConstantHelper.Builder<Builder> {

        private Builder() {
        }

        public ConstantPrincipalDecoder build() {
            return new ConstantPrincipalDecoder(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
