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
 * Represantation of an otp-credential-mapper configuration in ldap-realm/identity-mapping.
 *
 * @author Josef Cacek
 */
public class OtpCredentialMapper implements ModelNodeConvertable {

    private final String algorithmFrom;
    private final String hashFrom;
    private final String seedFrom;
    private final String sequenceFrom;

    private OtpCredentialMapper(Builder builder) {
        this.algorithmFrom = Objects.requireNonNull(builder.algorithmFrom);
        this.hashFrom = Objects.requireNonNull(builder.hashFrom);
        this.seedFrom = Objects.requireNonNull(builder.seedFrom);
        this.sequenceFrom = Objects.requireNonNull(builder.sequenceFrom);
    }

    @Override
    public ModelNode toModelNode() {
        ModelNode modelNode = new ModelNode();
        setIfNotNull(modelNode, "algorithm-from", algorithmFrom);
        setIfNotNull(modelNode, "hash-from", hashFrom);
        setIfNotNull(modelNode, "seed-from", seedFrom);
        setIfNotNull(modelNode, "sequence-from", sequenceFrom);
        return modelNode;
    }

    /**
     * Creates builder to build {@link OtpCredentialMapper}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link OtpCredentialMapper}.
     */
    public static final class Builder {
        private String algorithmFrom;
        private String hashFrom;
        private String seedFrom;
        private String sequenceFrom;

        private Builder() {
        }

        public Builder withAlgorithmFrom(String algorithmFrom) {
            this.algorithmFrom = algorithmFrom;
            return this;
        }

        public Builder withHashFrom(String hashFrom) {
            this.hashFrom = hashFrom;
            return this;
        }

        public Builder withSeedFrom(String seedFrom) {
            this.seedFrom = seedFrom;
            return this;
        }

        public Builder withSequenceFrom(String sequenceFrom) {
            this.sequenceFrom = sequenceFrom;
            return this;
        }

        public OtpCredentialMapper build() {
            return new OtpCredentialMapper(this);
        }
    }
}
