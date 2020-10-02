/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
