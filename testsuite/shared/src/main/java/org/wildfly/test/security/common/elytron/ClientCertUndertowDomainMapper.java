/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.common.elytron;

import java.util.Objects;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Creates Undertow mapping to Elytron security domain. The Undertow-to-Elytron mapping goes from Undertow
 * {@code application-security-domain} configuration to Elytron's {@code http-authentication-factory}
 * (which uses a {@code http-server-mechanism-factory}) with CLIENT-CERT mechanism set.
 * The {@code http-authentication-factory} then references Elytron security domain to be used.
 *
 * @author Ondrej Kotek
 */
public class ClientCertUndertowDomainMapper extends AbstractConfigurableElement {

    private final String securityDomain;

    private ClientCertUndertowDomainMapper(Builder builder) {
        super(builder);
        this.securityDomain = Objects.requireNonNull(builder.securityDomain, "Security domain name has to be provided");
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/provider-http-server-mechanism-factory=test:add()
        cli.sendLine(String.format("/subsystem=elytron/provider-http-server-mechanism-factory=%s:add()", name));

        // /subsystem=elytron/http-authentication-factory=test:add(security-domain=test,
        // http-server-mechanism-factory=test,
        // mechanism-configurations=[{mechanism-name=CLIENT-CERT,mechanism-realm-configurations=[{realm-name=test}]}])
        cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%1$s:add(security-domain=%2$s,"
                + "http-server-mechanism-factory=%1$s,"
                + "mechanism-configurations=[{mechanism-name=CLIENT_CERT,mechanism-realm-configurations=[{realm-name=%1$s}]}])",
                name, securityDomain));

        // /subsystem=undertow/application-security-domain=test:add(http-authentication-factory=test)
        cli.sendLine(String.format(
                "/subsystem=undertow/application-security-domain=%1$s:add(http-authentication-factory=%1$s)",
                name));
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", name));
        cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()", name));
        cli.sendLine(String.format("/subsystem=elytron/provider-http-server-mechanism-factory=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link ClientCertUndertowDomainMapper}.
     * The name attribute is used for http-authentication-factory and provider-http-server-mechanism-factory names.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link ClientCertUndertowDomainMapper}.
     * The name attribute is used for http-authentication-factory and provider-http-server-mechanism-factory names.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String securityDomain;

        private Builder() {
        }


        public Builder withSecurityDomain(String securityDomain) {
            this.securityDomain = securityDomain;
            return this;
        }

        public ClientCertUndertowDomainMapper build() {
            return new ClientCertUndertowDomainMapper(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
