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

import org.jboss.dmr.ModelNode;

/**
 * Represantation of an x509-credential-mapper configuration in ldap-realm/identity-mapping.
 *
 * @author Josef Cacek
 */
public class X509CredentialMapper implements ModelNodeConvertable {

    private final String digestFrom;
    private final String digestAlgorithm;
    private final String certificateFrom;
    private final String serialNumberFrom;
    private final String subjectDnFrom;

    private X509CredentialMapper(Builder builder) {
        this.digestFrom = builder.digestFrom;
        this.digestAlgorithm = builder.digestAlgorithm;
        this.certificateFrom = builder.certificateFrom;
        this.serialNumberFrom = builder.serialNumberFrom;
        this.subjectDnFrom = builder.subjectDnFrom;
    }

    @Override
    public ModelNode toModelNode() {
        ModelNode modelNode = new ModelNode();
        setIfNotNull(modelNode, "digest-from", digestFrom);
        setIfNotNull(modelNode, "digest-algorithm", digestAlgorithm);
        setIfNotNull(modelNode, "certificate-from", certificateFrom);
        setIfNotNull(modelNode, "serial-number-from", serialNumberFrom);
        setIfNotNull(modelNode, "subject-dn-from", subjectDnFrom);
        return modelNode;
    }

    /**
     * Creates builder to build {@link X509CredentialMapper}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link X509CredentialMapper}.
     */
    public static final class Builder {
        private String digestFrom;
        private String digestAlgorithm;
        private String certificateFrom;
        private String serialNumberFrom;
        private String subjectDnFrom;

        private Builder() {
        }

        public Builder withDigestFrom(String digestFrom) {
            this.digestFrom = digestFrom;
            return this;
        }

        public Builder withDigestAlgorithm(String digestAlgorithm) {
            this.digestAlgorithm = digestAlgorithm;
            return this;
        }

        public Builder withCertificateFrom(String certificateFrom) {
            this.certificateFrom = certificateFrom;
            return this;
        }

        public Builder withSerialNumberFrom(String serialNumberFrom) {
            this.serialNumberFrom = serialNumberFrom;
            return this;
        }

        public Builder withSubjectDnFrom(String subjectDnFrom) {
            this.subjectDnFrom = subjectDnFrom;
            return this;
        }

        public X509CredentialMapper build() {
            return new X509CredentialMapper(this);
        }
    }
}
