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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * Elytron 'dir-context' configuration.
 *
 * @author Josef Cacek
 */
public class DirContext extends AbstractConfigurableElement {

    private final String url;
    private final String authenticationContext;
    private final String authenticationLevel;
    private final Long connectionTimeout;
    private final CredentialReference credentialReference;
    private final Boolean enableConnectionPooling;
    private final String module;
    private final String principal;
    private final Map<String, String> properties;
    private final Long readTimeout;
    private final String referralMode;
    private final String sslContext;

    private DirContext(Builder builder) {
        super(builder);
        this.url = Objects.requireNonNull(builder.url, "URL in Elytron dir-context configuration has to be provided.");
        this.authenticationContext = builder.authenticationContext;
        this.authenticationLevel = builder.authenticationLevel;
        this.connectionTimeout = builder.connectionTimeout;
        this.credentialReference = builder.credentialReference;
        this.enableConnectionPooling = builder.enableConnectionPooling;
        this.module = builder.module;
        this.principal = builder.principal;
        this.properties = new HashMap<>(builder.properties);
        this.readTimeout = builder.readTimeout;
        this.referralMode = builder.referralMode;
        this.sslContext = builder.sslContext;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode op = Util
                .createAddOperation(PathAddress.pathAddress().append("subsystem", "elytron").append("dir-context", name));
        op.get("url").set(url);
        setIfNotNull(op, "authentication-context", authenticationContext);
        setIfNotNull(op, "authentication-level", authenticationLevel);
        setIfNotNull(op, "connection-timeout", connectionTimeout);
        setIfNotNull(op, "credential-reference", credentialReference);
        setIfNotNull(op, "enable-connection-pooling", enableConnectionPooling);
        setIfNotNull(op, "module", module);
        setIfNotNull(op, "principal", principal);
        setIfNotNull(op, "properties", properties);
        setIfNotNull(op, "read-timeout", readTimeout);
        setIfNotNull(op, "referral-mode", referralMode);
        setIfNotNull(op, "ssl-context", sslContext);

        Utils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(
                PathAddress.pathAddress().append("subsystem", "elytron").append("dir-context", name)), client);
    }

    /**
     * Creates builder to build {@link DirContext}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link DirContext}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String authenticationContext;
        private String authenticationLevel;
        private Long connectionTimeout;
        private CredentialReference credentialReference;
        private Boolean enableConnectionPooling;
        private String module;
        private String principal;
        private Map<String, String> properties = new HashMap<String, String>();
        private Long readTimeout;
        private String referralMode;
        private String sslContext;
        private String url;

        private Builder() {
        }

        public Builder withAuthenticationContext(String authenticationContext) {
            this.authenticationContext = authenticationContext;
            return self();
        }

        public Builder withAuthenticationLevel(String authenticationLevel) {
            this.authenticationLevel = authenticationLevel;
            return self();
        }

        public Builder withConnectionTimeout(Long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return self();
        }

        public Builder withCredentialReference(CredentialReference credentialReference) {
            this.credentialReference = credentialReference;
            return self();
        }

        public Builder withEnableConnectionPooling(Boolean enableConnectionPooling) {
            this.enableConnectionPooling = enableConnectionPooling;
            return self();
        }

        public Builder withModule(String module) {
            this.module = module;
            return self();
        }

        public Builder withPrincipal(String principal) {
            this.principal = principal;
            return self();
        }

        public Builder addProperty(String key, String value) {
            this.properties.put(key, value);
            return self();
        }

        public Builder withReadTimeout(Long readTimeout) {
            this.readTimeout = readTimeout;
            return self();
        }

        public Builder withReferralMode(String referralMode) {
            this.referralMode = referralMode;
            return self();
        }

        public Builder withSslContext(String sslContext) {
            this.sslContext = sslContext;
            return self();
        }

        public Builder withUrl(String url) {
            this.url = url;
            return self();
        }

        public DirContext build() {
            return new DirContext(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
