/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.security.common.elytron;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import java.util.Objects;

import org.jboss.dmr.ModelNode;

/**
 * Represantation of a user-password-mapper configuration in ldap-realm/identity-mapping.
 *
 * @author Josef Cacek
 */
public class UserPasswordMapper implements ModelNodeConvertable {

    private final String from;
    private final Boolean writable;
    private final Boolean verifiable;

    private UserPasswordMapper(Builder builder) {
        this.from = Objects.requireNonNull(builder.from, "The 'from' attribute has to be provided.");
        this.writable = builder.writable;
        this.verifiable = builder.verifiable;
    }

    @Override
    public ModelNode toModelNode() {
        ModelNode modelNode = new ModelNode();
        setIfNotNull(modelNode, "from", from);
        setIfNotNull(modelNode, "writable", writable);
        setIfNotNull(modelNode, "verifiable", verifiable);
        return modelNode;
    }

    /**
     * Creates builder to build {@link UserPasswordMapper}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link UserPasswordMapper}.
     */
    public static final class Builder {
        private String from;
        private Boolean writable;
        private Boolean verifiable;

        private Builder() {
        }

        public Builder withFrom(String from) {
            this.from = from;
            return this;
        }

        public Builder withWritable(Boolean writable) {
            this.writable = writable;
            return this;
        }

        public Builder withVerifiable(Boolean verifiable) {
            this.verifiable = verifiable;
            return this;
        }

        public UserPasswordMapper build() {
            return new UserPasswordMapper(this);
        }
    }
}
