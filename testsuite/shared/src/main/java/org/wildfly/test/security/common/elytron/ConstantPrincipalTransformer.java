/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.common.elytron;

/**
 * Elytron constant-principal-transformer configuration implementation.
 *
 * @author Josef Cacek
 */
public class ConstantPrincipalTransformer extends AbstractConstantHelper {

    private ConstantPrincipalTransformer(Builder builder) {
        super(builder);
    }


    @Override
    protected String getConstantElytronType() {
        return "constant-principal-transformer";
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

        public ConstantPrincipalTransformer build() {
            return new ConstantPrincipalTransformer(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
