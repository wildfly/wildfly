/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Helper class for adding "ocsp" attribute.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
public class Ocsp implements CliFragment {

    public static final Ocsp EMPTY = Ocsp.builder().build();

    private final String responder;
    private final String responderKeyStore;
    private final String responderCertificate;
    private final boolean preferCrls;

    private Ocsp(Builder builder) {
        this.responder = builder.responder;
        this.responderKeyStore = builder.responderKeyStore;
        this.responderCertificate = builder.responderCertificate;
        this.preferCrls = builder.preferCrls;
    }

    @Override
    public String asString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ocsp={ ");

        if (isNotBlank(responder)) {
            sb.append(String.format("responder=\"%s\", ", responder));
        }
        if (isNotBlank(responderKeyStore)) {
            sb.append(String.format("responder-keystore=\"%s\", ", responderKeyStore));
        }
        if (isNotBlank(responderCertificate)) {
            sb.append(String.format("responder-certificate=\"%s\", ", responderCertificate));
        }
        sb.append(String.format("prefer-crls=\"%s\", ", preferCrls));

        sb.append("}, ");
        return sb.toString();
    }

    /**
     * Creates builder to build {@link Ocsp}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link Ocsp}.
     */
    public static final class Builder {
        private String responder;
        private String responderKeyStore;
        private String responderCertificate;
        private boolean preferCrls;

        private Builder() {
        }

        public Builder withResponder(String responder) {
            this.responder = responder;
            return this;
        }
        public Builder withResponderKeyStore(String responderKeyStore) {
            this.responderKeyStore = responderKeyStore;
            return this;
        }
        public Builder withResponderCertificate(String responderCertificate) {
            this.responderCertificate = responderCertificate;
            return this;
        }

        public Builder withPreferCrls(boolean preferCrls) {
            this.preferCrls = preferCrls;
            return this;
        }

        public Ocsp build() {
            return new Ocsp(this);
        }
    }

}
