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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.security.common.elytron;

/**
 * Helper class for adding "match-rules" attributes into CLI commands.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class MatchRules implements CliFragment {

    private final String authenticationConfiguration;
    private final String matchAbstractType;
    private final String matchAbstractTypeAuthority;
    private final String matchHost;
    private final String matchLocalSecurityDomain;
    private final Boolean matchNoUser;
    private final String matchPath;
    private final String matchPort;
    private final String matchProtocol;
    private final String matchPurpose;
    private final String matchUrn;
    private final String matchUser;
    private final String sslContext;

    private MatchRules(final Builder builder) {
        this.authenticationConfiguration = builder.authenticationConfiguration;
        this.matchAbstractType = builder.matchAbstractType;
        this.matchAbstractTypeAuthority = builder.matchAbstractTypeAuthority;
        this.matchHost = builder.matchHost;
        this.matchLocalSecurityDomain = builder.matchLocalSecurityDomain;
        this.matchNoUser = builder.matchNoUser;
        this.matchPath = builder.matchPath;
        this.matchPort = builder.matchPort;
        this.matchProtocol = builder.matchProtocol;
        this.matchPurpose = builder.matchPurpose;
        this.matchUrn = builder.matchUrn;
        this.matchUser = builder.matchUser;
        this.sslContext = builder.sslContext;
    }

    @Override
    public String asString() {
        StringBuilder sb = new StringBuilder("match-rules=[{");
        if (authenticationConfiguration != null && !authenticationConfiguration.isEmpty()) {
            sb.append(String.format("authentication-configuration=\"%s\", ", authenticationConfiguration));
        }
        if (matchAbstractType != null && !matchAbstractType.isEmpty()) {
            sb.append(String.format("match-abstract-type=\"%s\", ", matchAbstractType));
        }
        if (matchAbstractTypeAuthority != null && !matchAbstractTypeAuthority.isEmpty()) {
            sb.append(String.format("match-abstract-type-authority=\"%s\", ", matchAbstractTypeAuthority));
        }
        if (matchHost != null && !matchHost.isEmpty()) {
            sb.append(String.format("match-host=\"%s\", ", matchHost));
        }
        if (matchLocalSecurityDomain != null && !matchLocalSecurityDomain.isEmpty()) {
            sb.append(String.format("match-local-security-domain=\"%s\", ", matchLocalSecurityDomain));
        }
        if (matchNoUser != null) {
            sb.append(String.format("match-no-user=\"%s\", ", matchNoUser.toString()));
        }
        if (matchPath != null && !matchPath.isEmpty()) {
            sb.append(String.format("match-path=\"%s\", ", matchPath));
        }
        if (matchPort != null && !matchPort.isEmpty()) {
            sb.append(String.format("match-port=\"%s\", ", matchPort));
        }
        if (matchProtocol != null && !matchProtocol.isEmpty()) {
            sb.append(String.format("match-protocol=\"%s\", ", matchProtocol));
        }
        if (matchPurpose != null && !matchPurpose.isEmpty()) {
            sb.append(String.format("match-purpose=\"%s\", ", matchPurpose));
        }
        if (matchUrn != null && !matchUrn.isEmpty()) {
            sb.append(String.format("match-urn=\"%s\", ", matchUrn));
        }
        if (matchUser != null && !matchUser.isEmpty()) {
            sb.append(String.format("match-user=\"%s\", ", matchUser));
        }
        if (sslContext != null && !sslContext.isEmpty()) {
            sb.append(String.format("ssl-context=\"%s\", ", sslContext));
        }
        sb.append("}], ");
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String authenticationConfiguration;
        private String matchAbstractTypeAuthority;
        private String matchAbstractType;
        private String matchHost;
        private String matchLocalSecurityDomain;
        private Boolean matchNoUser;
        private String matchPath;
        private String matchPort;
        private String matchProtocol;
        private String matchPurpose;
        private String matchUrn;
        private String matchUser;
        private String sslContext;

        private Builder() {
        }

        public Builder withAuthenticationConfiguration(final String authenticationConfiguration) {
            this.authenticationConfiguration = authenticationConfiguration;
            return this;
        }

        public Builder withMatchAbstractType(final String matchAbstractType) {
            this.matchAbstractType = matchAbstractType;
            return this;
        }

        public Builder withMatchAbstractTypeAuthority(final String matchAbstractTypeAuthority) {
            this.matchAbstractTypeAuthority = matchAbstractTypeAuthority;
            return this;
        }

        public Builder withMatchHost(final String matchHost) {
            this.matchHost = matchHost;
            return this;
        }

        public Builder withMatchLocalSecurityDomain(final String matchLocalSecurityDomain) {
            this.matchLocalSecurityDomain = matchLocalSecurityDomain;
            return this;
        }

        public Builder withMatchNoUser(final Boolean matchNoUser) {
            this.matchNoUser = matchNoUser;
            return this;
        }

        public Builder withMatchPath(final String matchPath) {
            this.matchPath = matchPath;
            return this;
        }

        public Builder withMatchPort(final String matchPort) {
            this.matchPort = matchPort;
            return this;
        }

        public Builder withMatchProtocol(final String matchProtocol) {
            this.matchProtocol = matchProtocol;
            return this;
        }

        public Builder withMatchPurpose(final String matchPurpose) {
            this.matchPurpose = matchPurpose;
            return this;
        }

        public Builder withMatchUrn(final String matchUrn) {
            this.matchUrn = matchUrn;
            return this;
        }

        public Builder withMatchUser(final String matchUser) {
            this.matchUser = matchUser;
            return this;
        }

        public Builder withSslContext(final String sslContext) {
            this.sslContext = sslContext;
            return this;
        }


        public MatchRules build() {
            return new MatchRules(this);
        }
    }

}
