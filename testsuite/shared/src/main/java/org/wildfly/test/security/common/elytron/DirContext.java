/*
 * Copyright 2020 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ConfigurableElement} to define the dir-context resource within the Elytron subsystem.
 *
 * @author Ondrej Kotek
 */
public class DirContext implements ConfigurableElement {

    private final PathAddress address;
    private final String name;
    private final String url;
    private final AuthenticationLevel authenticationLevel;
    private final String principal;
    private final Boolean enableConnectionPooling;
    private final String sslContext;
    private final ReferralMode referralMode;
    private final String authenticationContext;
    private final Integer connectionTimeout;
    private final Integer readTimeout;
    private final String module;
    private final List<Property> properties;
    private final CredentialReference credentialReference;

    DirContext(Builder builder) {
        this.name = builder.name;
        this.address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("dir-context", name));
        this.url = builder.url;
        this.authenticationLevel = builder.authenticationLevel;
        this.principal = builder.principal;
        this.enableConnectionPooling = builder.enableConnectionPooling;
        this.sslContext = builder.sslContext;
        this.referralMode = builder.referralMode;
        this.authenticationContext = builder.authenticationContext;
        this.connectionTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
        this.module = builder.module;
        this.properties = builder.properties;
        this.credentialReference = builder.credentialReference;
    }

    @Override
    public String getName() {
        return name;
    }

    public ModelNode getAddOperation() {
        ModelNode addOperation = Util.createAddOperation(address);
        addOperation.get("dir-context");
        if (url != null) {
            addOperation.get("url").set(url);
        }
        if (authenticationLevel != null) {
            addOperation.get("authentication-level").set(authenticationLevel == null ? null : authenticationLevel.name());
        }
        if (principal != null) {
            addOperation.get("principal").set(principal);
        }
        if (enableConnectionPooling != null) {
            addOperation.get("enable-connection-pooling").set(enableConnectionPooling);
        }
        if (sslContext != null) {
            addOperation.get("ssl-context").set(sslContext);
        }
        if (referralMode != null) {
            addOperation.get("referral-mode").set(referralMode == null ? null : referralMode.name());
        }
        if (authenticationContext != null) {
            addOperation.get("authentication-context").set(authenticationContext);
        }
        if (connectionTimeout != null) {
            addOperation.get("connection-timeout").set(connectionTimeout);
        }
        if (readTimeout != null) {
            addOperation.get("read-timeout").set(readTimeout);
        }
        if (module != null) {
            addOperation.get("module").set(module);
        }
        if (properties != null && !properties.isEmpty()) {
            ModelNode propertiesNode = new ModelNode();
            for (Property property : properties) {
                propertiesNode.add(property.getKey(), property.getValue());
            }
            addOperation.get("properties").set(propertiesNode.asObject());
        }
        if (credentialReference != null) {
            addOperation.get("credential-reference").set(credentialReference.asModelNode());
        }
        return addOperation;
    }

    public ModelNode getRemoveOperation() {
        return Util.createRemoveOperation(address);
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(getAddOperation(), client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(getRemoveOperation(), client);
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public static final class Builder {

        private final String name;
        private String url;
        private AuthenticationLevel authenticationLevel;
        private String principal;
        private Boolean enableConnectionPooling;
        private String sslContext;
        private ReferralMode referralMode;
        private String authenticationContext;
        private Integer connectionTimeout;
        private Integer readTimeout;
        private String module;
        private List<Property> properties = new ArrayList<Property>();
        private CredentialReference credentialReference;

        public Builder(String name) {
            this.name = name;
        }

        public Builder withUrl(String url) {
            this.url = url;

            return this;
        }

        public Builder withAuthenticationLevel(AuthenticationLevel authenticationLevel) {
            this.authenticationLevel = authenticationLevel;

            return this;
        }

        public Builder withPrincipal(String principal) {
            this.principal = principal;

            return this;
        }

        public Builder withEnableConnectionPooling(boolean enableConnectionPooling) {
            this.enableConnectionPooling = enableConnectionPooling;

            return this;
        }

        public Builder withSslContext(String sslContext) {
            this.sslContext = sslContext;

            return this;
        }

        public Builder withReferralMode(ReferralMode referralMode) {
            this.referralMode = referralMode;

            return this;
        }

        public Builder withAuthenticationContext(String authenticationContext) {
            this.authenticationContext = authenticationContext;

            return this;
        }

        public Builder withConnectionTimeout(Integer connectionTimeout) {
            this.connectionTimeout = connectionTimeout;

            return this;
        }

        public Builder withReadTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;

            return this;
        }

        public Builder withModule(String module) {
            this.module = module;

            return this;
        }

        public Builder withProperties(Property... properties) {
            Collections.addAll(this.properties, properties);

            return this;
        }

        public Builder withCredentialReference(CredentialReference credentialReference) {
            this.credentialReference = credentialReference;

            return this;
        }

        public DirContext build() {
            return new DirContext(this);
        }
    }

    public static class Property {

        private final String key;
        private final String value;

        public Property(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
    }

        public String getValue() {
            return value;
        }

    }

    public static enum AuthenticationLevel {

        NONE, SIMPLE, STRONG
    }

    public static enum ReferralMode {

        FOLLOW, IGNORE, THROW
    }
}
