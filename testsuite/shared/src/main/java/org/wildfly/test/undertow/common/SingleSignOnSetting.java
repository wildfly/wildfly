/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.undertow.common;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.elytron.CredentialReference;

/**
 * A representation of the single sign on settings within the Undertow application-security-domain resource.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SingleSignOnSetting {

    private final String clientSSLContext;
    private final String cookieName;
    private final CredentialReference credentialReference;
    private final String domain;
    private final boolean httpOnly;
    private final String keyAlias;
    private final String keyStore;
    private final String path;
    private final boolean secure;

    private SingleSignOnSetting(Builder builder) {
        this.clientSSLContext = builder.clientSSLContext;
        this.cookieName = builder.cookieName;
        this.credentialReference = builder.credentialReference;
        this.domain = builder.domain;
        this.httpOnly = builder.httpOnly;
        this.keyAlias = builder.keyAlias;
        this.keyStore = builder.keyStore;
        this.path = builder.path;
        this.secure = builder.secure;
    }

    /**
     * Create and return a {@code ModelNode} to add the single sign on settings.
     *
     * This is intended to be used in conjunction with {@link UndertowApplicationSecurityDomain}
     * which will embed the add operation into a composite operation.  Remove is not needed as
     * the parent will be removed instead.
     *
     * @param parentAddress the address of the parent resource.
     * @return the resulting add operation {@code ModelNode}.
     */
    ModelNode getAddOperation(final PathAddress parentAddress) {
        PathAddress settingsAddress = PathAddress.pathAddress(parentAddress,PathElement.pathElement("setting", "single-sign-on"));
        final ModelNode response = Util.createAddOperation(settingsAddress);
        setIfNotNull(response, "client-ssl-context", clientSSLContext);
        setIfNotNull(response, "cookie-name", cookieName);
        if (credentialReference != null) {
            response.get("credential-reference").set(credentialReference.asModelNode());
        }
        setIfNotNull(response, "domain", domain);
        if (httpOnly) {
            response.get("http-only").set(true);
        }
        setIfNotNull(response, "key-alias", keyAlias);
        setIfNotNull(response, "key-store", keyStore);
        setIfNotNull(response, "path", path);
        if (secure) {
            response.get("secure").set(true);
        }

        return response;
    }

    /**
     * Create a new builder for a {@link SingleSignOnSetting}.
     *
     * @return a new Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String clientSSLContext;
        private String cookieName;
        private CredentialReference credentialReference;
        private String domain;
        private boolean httpOnly = false;
        private String keyAlias;
        private String keyStore;
        private String path;
        private boolean secure = false;

        Builder() {} // Create from builder() factory method.

        /**
         * Set the client SSL context.
         *
         * @param clientSSLContext the client SSL context.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withClientSSLContext(final String clientSSLContext) {
            this.clientSSLContext = clientSSLContext;

            return this;
        }

        /**
         * Set the cookie name.
         *
         * @param cookieName the cookie name.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withCookieName(final String cookieName) {
            this.cookieName = cookieName;

            return this;
        }

        /**
         * Set the credential reference.
         *
         * @param credentialReference the credential reference.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withCredentialReference(final CredentialReference credentialReference) {
            this.credentialReference = credentialReference;

            return this;
        }

        /**
         * Set the domain.
         *
         * @param domain the domain.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withDomain(final String domain) {
            this.domain = domain;

            return this;
        }

        /**
         * Set the http only flag.
         *
         * @param httpOnly the http only flag.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withHttpOnly(final boolean httpOnly) {
            this.httpOnly = httpOnly;

            return this;
        }

        /**
         * Set the key alias.
         *
         * @param keyAlias the key alias.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withKeyAlias(final String keyAlias) {
            this.keyAlias = keyAlias;

            return this;
        }

        /**
         * Set the key store.
         *
         * @param keyStore the key store.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withKeyStore(final String keyStore) {
            this.keyStore = keyStore;

            return this;
        }

        /**
         * Set the path.
         *
         * @param path the path.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withPath(final String path) {
            this.path = path;

            return this;
        }

        /**
         * Set the secure flag.
         *
         * @param secure the secure flag.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withSecure(final boolean secure) {
            this.secure = secure;

            return this;
        }

        /**
         * Build the {@link SingleSignOnSetting}.
         *
         * @return the built {@link SingleSignOnSetting}.
         */
        public SingleSignOnSetting build() {
            return new SingleSignOnSetting(this);
        }

    }
}
