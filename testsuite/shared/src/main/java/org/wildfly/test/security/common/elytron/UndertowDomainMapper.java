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

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Creates Undertow mapping to Elytron security domain. The Undertow-to-Elytron mapping goes from Undertow
 * {@code applicationDomain} configuration to Elytron's {@code http-authentication-factory} (which uses a
 * {@code http-server-mechanism-factor}). The {@code http-authentication-factory} then references Elytron security domain to be
 * used.
 * <p>
 * Configuration of this undertow mapping may contain set of application domains names (referenced as {@code security-domain}s
 * in {@code jboss-web.xml}) to be mapped to given security domain name. If applicationDomains is not configured for the mapper
 * (i.e. it's empty), then just one mapping is created with applicationDomain name the same as Elytron security domain name. If
 * the applicationDomains attribute is not empty, then just the provided names are mapped (i.e. the Elytron domain name is not
 * used automatically in such case).
 * </p>
 *
 * @author Josef Cacek
 */
public class UndertowDomainMapper  extends AbstractConfigurableElement implements SecurityDomainMapper {

    private final Set<String> applicationDomains;

    private UndertowDomainMapper(Builder builder) {
        super(builder);
        this.applicationDomains = new HashSet<>(builder.applicationDomains);
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/provider-http-server-mechanism-factory=test:add()
        cli.sendLine(String.format("/subsystem=elytron/provider-http-server-mechanism-factory=%s:add()", name));

        // /subsystem=elytron/http-authentication-factory=test:add(security-domain=test,
        // http-server-mechanism-factory=test,
        // mechanism-configurations=[
        // {mechanism-name=DIGEST,mechanism-realm-configurations=[{realm-name=test}]},
        // {mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=test}]},
        // {mechanism-name=FORM]}])

        cli.sendLine(String
                .format("/subsystem=elytron/http-authentication-factory=%1$s:add(security-domain=%1$s, http-server-mechanism-factory=%1$s,"
                        + "  mechanism-configurations=["
                        + "    {mechanism-name=DIGEST,mechanism-realm-configurations=[{realm-name=%1$s}]},"
                        + "    {mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=%1$s}]},"
                        + "    {mechanism-name=FORM}])", name));

        if (applicationDomains.isEmpty()) {
            // /subsystem=undertow/application-security-domain=test:add(http-authentication-factory=test)
            cli.sendLine(String.format(
                    "/subsystem=undertow/application-security-domain=%1$s:add(http-authentication-factory=%1$s)", name));
        } else {
            for (String appDomain : applicationDomains) {
                // /subsystem=undertow/application-security-domain=appdomain:add(http-authentication-factory=test)
                cli.sendLine(
                        String.format("/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)",
                                appDomain, name));
            }
        }
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        if (applicationDomains.isEmpty()) {
            cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", name));
        } else {
            for (String appDomain : applicationDomains) {
                cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", appDomain));
            }
        }
        cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()", name));
        cli.sendLine(String.format("/subsystem=elytron/provider-http-server-mechanism-factory=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link UndertowDomainMapper}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link UndertowDomainMapper}. The name attribute is used for security domain name.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private Set<String> applicationDomains = new HashSet<>();

        private Builder() {
        }

        /**
         * Adds given application domains to the configuration.
         */
        public Builder withApplicationDomains(String... applicationDomains) {
            if (applicationDomains != null) {
                for (String domain : applicationDomains) {
                    this.applicationDomains.add(domain);
                }
            }
            return this;
        }

        public UndertowDomainMapper build() {
            return new UndertowDomainMapper(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
