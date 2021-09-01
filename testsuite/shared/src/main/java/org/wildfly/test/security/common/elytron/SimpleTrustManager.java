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

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * Elytron trust-managers configuration implementation.
 *
 * @author Josef Cacek
 */
public class SimpleTrustManager extends AbstractConfigurableElement implements TrustManager {

    private final String keyStore;
    private final String algorithm;
    private final int maximumCertPath;
    private final Boolean onlyLeafCert;
    private final Boolean softFail;
    private final Ocsp ocsp;
    private final CertificateRevocationList crl;
    private final List<CertificateRevocationList> crls;

    private SimpleTrustManager(Builder builder) {
        super(builder);
        this.keyStore = Objects.requireNonNull(builder.keyStore, "Key-store name has to be provided");
        this.algorithm = builder.algorithm;
        this.maximumCertPath = builder.maximumCertPath;
        this.softFail = builder.softFail;
        this.onlyLeafCert = builder.onlyLeafCert;
        this.ocsp = builder.ocsp;
        this.crl = builder.crl;
        this.crls = builder.crls;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        StringBuilder cliLine = new StringBuilder("/subsystem=elytron/trust-manager=").append(name).append(":add(");

        // Already appends ',' after itself if defined.
        if (ocsp != null) {
            cliLine.append(ocsp.asString());
        }

        // Already appends ',' after itself if defined.
        if (crl != null) {
            cliLine.append("certificate-revocation-list=");
            cliLine.append(crl.asString());
            cliLine.append(", ");
        }

        // certificate-revocation-list and certificate-revocation-lists are mutually exclusive
        else if (crls != null) {
            cliLine.append("certificate-revocation-lists=[");
            for (int i = 0; i < crls.size(); i++) {
                cliLine.append(crls.get(i).asString());

                // only append comma as long as there is another CRL to add to list
                if (i != crls.size() - 1) cliLine.append(", ");
            }
            cliLine.append("], ");
        }

        if (StringUtils.isNotBlank(keyStore)) {
            cliLine.append("key-store=\"").append(keyStore).append("\"");
        }

        String alg;
        if (StringUtils.isNotBlank(algorithm)) {
            alg = algorithm;
        } else {
            alg = SystemUtils.JAVA_VENDOR.toUpperCase(Locale.ENGLISH).contains("IBM") ? "IBMX509" : "SunX509";
        }
        cliLine.append(",algorithm=\"").append(alg).append("\"");

        if (softFail != null) {
            cliLine.append(",soft-fail=\"").append(softFail).append("\"");
        }

        if (onlyLeafCert != null) {
            cliLine.append(",only-leaf-cert=\"").append(onlyLeafCert).append("\"");
        }

        if (maximumCertPath > -1) {
            cliLine.append(",maximum-cert-path=\"").append(maximumCertPath).append("\"");
        }

        cliLine.append(")");

        cli.sendLine(cliLine.toString());
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/trust-manager=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link SimpleTrustManager}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleTrustManager}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String keyStore;
        private String algorithm;
        private int maximumCertPath = -1;
        private Boolean onlyLeafCert;
        private Boolean softFail;
        private Ocsp ocsp;
        private CertificateRevocationList crl;
        private List<CertificateRevocationList> crls;

        private Builder() {
        }

        public Builder withKeyStore(String keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        public Builder withAlgorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder withMaximumCertPath(int maximumCertPath) {
            this.maximumCertPath = maximumCertPath;
            return this;
        }

        public Builder withOnlyLeafCert(boolean onlyLeafCert) {
            this.onlyLeafCert = onlyLeafCert;
            return this;
        }

        public Builder withSoftFail(boolean softFail) {
            this.softFail = softFail;
            return this;
        }

        public Builder withOcsp(Ocsp ocsp) {
            this.ocsp = ocsp;
            return this;
        }

        public Builder withCrl(CertificateRevocationList crl) {
            this.crl = crl;
            return this;
        }

        public Builder withCrls(List<CertificateRevocationList> crls) {
            this.crls = crls;
            return this;
        }

        public SimpleTrustManager build() {
            return new SimpleTrustManager(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
