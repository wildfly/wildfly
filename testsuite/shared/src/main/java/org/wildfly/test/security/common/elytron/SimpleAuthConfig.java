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

import java.util.Properties;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron authentication configuration implementation.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SimpleAuthConfig extends AbstractConfigurableElement {

    private final Boolean anonymous;
    private final Boolean allowAllMechanisms;
    private final String[] allowSaslMechanisms;
    private final String[] forbidSaslMechanisms;
    private final Properties mechanismProperties;
    private final String authenticationName;
    private final String authorizationName;
    private final CredentialReference credentialReference;
    private final String extendsAttr;
    private final String host;
    private final String port;
    private final String protocol;
    private final String realm;
    private final String securityDomain;

    private SimpleAuthConfig(final Builder builder) {
        super(builder);
        this.anonymous = builder.anonymous;
        this.allowAllMechanisms = builder.allowAllMechanisms;
        this.allowSaslMechanisms = builder.allowSaslMechanisms;
        this.forbidSaslMechanisms = builder.forbidSaslMechanisms;
        this.mechanismProperties = builder.mechanismProperties;
        this.authenticationName = builder.authenticationName;
        this.authorizationName = builder.authorizationName;
        this.credentialReference = builder.credentialReference;
        this.extendsAttr = builder.extendsAttr;
        this.host = builder.host;
        this.port = builder.port;
        this.protocol = builder.protocol;
        this.realm = builder.realm;
        this.securityDomain = builder.securityDomain;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        final StringBuilder sb = new StringBuilder("/subsystem=elytron/authentication-configuration=\"");
        sb.append(name).append("\":add(");
        if (anonymous != null) {
            sb.append(anonymous.booleanValue() == true ? "" : "!").append("anonymous, ");
        }
        if (allowAllMechanisms != null) {
            sb.append(allowAllMechanisms.booleanValue() == true ? "" : "!").append("allow-all-mechanisms, ");
        }
        if (allowSaslMechanisms != null && allowSaslMechanisms.length > 0) {
            sb.append("allow-sasl-mechanisms=[");
            for (String mech : allowSaslMechanisms) {
                sb.append(mech).append(" ");
            }
            sb.append("], ");
        }
        if (forbidSaslMechanisms != null && forbidSaslMechanisms.length > 0) {
            sb.append("forbid-sasl-mechanisms=[");
            for (String mech : forbidSaslMechanisms) {
                sb.append(mech).append(" ");
            }
            sb.append("], ");
        }
        if (mechanismProperties != null && !mechanismProperties.isEmpty()) {
            sb.append("mechanism-properties={");
            for (Object key : mechanismProperties.keySet()) {
                sb.append(key.toString()).append("=").append(mechanismProperties.getProperty(key.toString()));
                sb.append(", ");
            }
            sb.append("}, ");
        }
        if (authenticationName != null && !authenticationName.isEmpty()) {
            sb.append(String.format("authentication-name=\"%s\", ", authenticationName));
        }
        if (authorizationName != null && !authorizationName.isEmpty()) {
            sb.append(String.format("authorization-name=\"%s\", ", authorizationName));
        }
        if (credentialReference != null) {
            sb.append(credentialReference.asString());
        }
        if (extendsAttr != null && !extendsAttr.isEmpty()) {
            sb.append(String.format("extends=\"%s\", ", extendsAttr));
        }
        if (host != null && !host.isEmpty()) {
            sb.append(String.format("host=\"%s\", ", host));
        }
        if (port != null && !port.isEmpty()) {
            sb.append(String.format("port=\"%s\", ", port));
        }
        if (protocol != null && !protocol.isEmpty()) {
            sb.append(String.format("protocol=\"%s\", ", protocol));
        }
        if (realm != null && !realm.isEmpty()) {
            sb.append(String.format("realm=\"%s\", ", realm));
        }
        if (securityDomain != null && !securityDomain.isEmpty()) {
            sb.append(String.format("security-domain=\"%s\", ", securityDomain));
        }
        sb.append(")");
        cli.sendLine(sb.toString());
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/authentication-configuration=\"%s\":remove", name));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {

        private Boolean anonymous;
        private Boolean allowAllMechanisms;
        private String[] allowSaslMechanisms;
        private String[] forbidSaslMechanisms;
        private Properties mechanismProperties;
        private String authenticationName;
        private String authorizationName;
        private CredentialReference credentialReference;
        private String extendsAttr;
        private String host;
        private String port;
        private String protocol;
        private String realm;
        private String securityDomain;

        private Builder() {
        }

        public Builder allowAnonymous(final Boolean allowAnonymous) {
            this.anonymous = allowAnonymous;
            return this;
        }

        public Builder allowAllMechanisms(final Boolean allowAllMechanisms) {
            this.allowAllMechanisms = allowAllMechanisms;
            return this;
        }

        public Builder withAllowedSaslMechanisms(final String... allowedSaslMechanisms) {
            this.allowSaslMechanisms = allowedSaslMechanisms;
            return this;
        }

        public Builder withForbiddenSaslMechanisms(final String... forbiddenSaslMechanisms) {
            this.forbidSaslMechanisms = forbiddenSaslMechanisms;
            return this;
        }

        public Builder withMechanismProperties(final Properties mechanismProperties) {
            this.mechanismProperties = mechanismProperties;
            return this;
        }

        public Builder withAuthenticationName(final String authenticationName) {
            this.authenticationName = authenticationName;
            return this;
        }

        public Builder withAuthorizationName(final String authorizationName) {
            this.authorizationName = authorizationName;
            return this;
        }

        public Builder withCredentialReference(final CredentialReference credentialReference) {
            this.credentialReference = credentialReference;
            return this;
        }

        public Builder withExtends(final String extendsAttr) {
            this.extendsAttr = extendsAttr;
            return this;
        }

        public Builder withHost(final String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(final String port) {
            this.port = port;
            return this;
        }

        public Builder withProtocol(final String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder withRealm(final String realm) {
            this.realm = realm;
            return this;
        }

        public Builder withSecurityDomain(final String securityDomain) {
            this.securityDomain = securityDomain;
            return this;
        }

        public SimpleAuthConfig build() {
            return new SimpleAuthConfig(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
