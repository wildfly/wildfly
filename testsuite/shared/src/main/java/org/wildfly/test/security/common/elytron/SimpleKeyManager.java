/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.common.elytron;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.Objects;
import javax.net.ssl.KeyManagerFactory;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron key-manager configuration implementation.
 *
 * @author Josef Cacek
 */
public class SimpleKeyManager extends AbstractConfigurableElement implements KeyManager {

    private final String keyStore;
    private final CredentialReference credentialReference;
    private final String generateSelfSignedCertificateHost;

    private SimpleKeyManager(Builder builder) {
        super(builder);
        this.keyStore = Objects.requireNonNull(builder.keyStore, "Key-store name has to be provided");
        this.credentialReference = defaultIfNull(builder.credentialReference, CredentialReference.EMPTY);
        this.generateSelfSignedCertificateHost = builder.generateSelfSignedCertificateHost;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/key-manager=httpsKM:add(key-store=httpsKS,algorithm="SunX509",credential-reference={clear-text=secret})

        if (generateSelfSignedCertificateHost != null) {
            cli.sendLine(String.format("/subsystem=elytron/key-manager=%s:add(key-store=\"%s\",algorithm=\"%s\", %s,generate-self-signed-certificate-host=\"%s\")", name,
                    keyStore, KeyManagerFactory.getDefaultAlgorithm(), credentialReference.asString(), generateSelfSignedCertificateHost));
        } else {
            cli.sendLine(String.format("/subsystem=elytron/key-manager=%s:add(key-store=\"%s\",algorithm=\"%s\", %s)", name,
                    keyStore, KeyManagerFactory.getDefaultAlgorithm(), credentialReference.asString()));
        }
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/key-manager=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link SimpleKeyManager}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleKeyManager}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String keyStore;
        private CredentialReference credentialReference;
        private String generateSelfSignedCertificateHost;

        private Builder() {
        }

        public Builder withKeyStore(String keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        public Builder withCredentialReference(CredentialReference credentialReference) {
            this.credentialReference = credentialReference;
            return this;
        }

        public Builder withGenerateSelfSignedCertificateHost(String generateSelfSignedCertificateHost) {
            this.generateSelfSignedCertificateHost = generateSelfSignedCertificateHost;
            return this;
        }

        public SimpleKeyManager build() {
            return new SimpleKeyManager(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
