/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Object which holds user configuration (password, values).
 *
 * @author Josef Cacek
 */
public class UserWithAttributeValues {

    private final String name;
    private final String password;
    private final Set<String> values;

    private UserWithAttributeValues(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Username must be not-null");
        this.password = builder.password != null ? builder.password : builder.name;
        this.values = new HashSet<>(builder.values);
    }

    /**
     * Returns username.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns password as plain text.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set of roles to be assigned to the user.
     */
    public Set<String> getValues() {
        return values;
    }

    /**
     * Creates builder to build {@link UserWithAttributeValues}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link UserWithAttributeValues}.
     */
    public static final class Builder {
        private String name;
        private String password;
        private final Set<String> values = new HashSet<>();

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Add given attribute values to the builder. It doesn't replace existing values, but it adds given valuess to them.
         */
        public Builder withValues(Set<String> values) {
            if (values != null) {
                this.values.addAll(values);
            }
            return this;
        }

        /**
         * Add given values to the builder. It doesn't replace existing values, but it adds given value to them.
         */
        public Builder withValues(String... values) {
            if (values != null) {
                this.values.addAll(Arrays.asList(values));
            }
            return this;
        }

        /**
         * Clears set of already added roles.
         */
        public Builder clearValues() {
            this.values.clear();
            return this;
        }

        /**
         * Builds UserWithRoles instance.
         *
         * @return
         */
        public UserWithAttributeValues build() {
            return new UserWithAttributeValues(this);
        }
    }

}
