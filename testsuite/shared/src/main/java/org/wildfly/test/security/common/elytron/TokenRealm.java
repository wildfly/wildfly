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
import org.jboss.dmr.ModelNode;

/**
 * A {@link ConfigurableElement} to define the token-realm resource within the Elytron subsystem.
 *
 * @author Ondrej Kotek
 */
public class TokenRealm implements SecurityRealm {

    private final PathAddress address;
    private final String name;
    private final Jwt jwt;
    private final Oauth2Introspection oauth2Introspection;
    private final String principalClaim;

    TokenRealm(final String name, final Jwt jwt, final Oauth2Introspection oauth2Introspection, final String principalClaim) {
        this.address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("token-realm", name));
        this.name = name;
        this.jwt = jwt;
        this.oauth2Introspection = oauth2Introspection;
        this.principalClaim = principalClaim;
    }

    @Override
    public String getName() {
        return name;
    }

    public ModelNode getAddOperation() {
        ModelNode addOperation = Util.createAddOperation(address);
        addOperation.get("token-realm");
        if (principalClaim != null) {
            addOperation.get("principal-claim").set(principalClaim);
        }
        if (jwt != null) {
            ModelNode jwtProperties = new ModelNode();
            if (jwt.getAudience() != null) {
                ModelNode audienceList = jwtProperties.get("audience");
                for (String audienceEntry : jwt.getAudience()) {
                    audienceList.add(audienceEntry);
                }
            }
            if (jwt.getIssuer() != null) {
                ModelNode issuerList = jwtProperties.get("issuer");
                for (String issuerEntry : jwt.getIssuer()) {
                    issuerList.add(issuerEntry);
                }
            }
            if (jwt.getCertificate() != null) {
                jwtProperties.get("certificate").set(jwt.getCertificate());
            }
            if (jwt.getKeyStore() != null) {
                jwtProperties.get("key-store").set(jwt.getKeyStore());
            }
            if (jwt.getPublicKey() != null) {
                jwtProperties.get("public-key").set(jwt.getPublicKey());
            }
            if (jwtProperties.asListOrEmpty().isEmpty()) {
                addOperation.get("jwt").setEmptyObject();
            } else {
                addOperation.get("jwt").set(jwtProperties.asObject());
            }
        }
        if (oauth2Introspection != null) {
            ModelNode oauth2IntrospectionProperties = new ModelNode();
            if (oauth2Introspection.getClientId() != null) {
                oauth2IntrospectionProperties.get("client-id").set(oauth2Introspection.getClientId());
            }
            if (oauth2Introspection.getClientSecret() != null) {
                oauth2IntrospectionProperties.get("client-secret").set(oauth2Introspection.getClientSecret());
            }
            if (oauth2Introspection.getClientSslContext() != null) {
                oauth2IntrospectionProperties.get("client-ssl-context").set(oauth2Introspection.getClientSslContext());
            }
            if (oauth2Introspection.getHostNameVerificationPolicy() != null) {
                oauth2IntrospectionProperties.get("host-name-verification-policy").set(oauth2Introspection.getHostNameVerificationPolicy());
            }
            if (oauth2Introspection.getIntrospectionUrl() != null) {
                oauth2IntrospectionProperties.get("introspection-url").set(oauth2Introspection.getIntrospectionUrl());
            }
            if (oauth2IntrospectionProperties.asListOrEmpty().isEmpty()) {
                addOperation.get("oauth2-introspection").setEmptyObject();
            } else {
                addOperation.get("oauth2-introspection").set(oauth2IntrospectionProperties.asObject());
            }
        }
        return addOperation;
    }

    public ModelNode getRemoveOperation() {
        return Util.createRemoveOperation(address);
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        org.jboss.as.test.integration.security.common.Utils.applyUpdate(getAddOperation(), client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        org.jboss.as.test.integration.security.common.Utils.applyUpdate(getRemoveOperation(), client);
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public static JwtBuilder jwtBuilder() {
        return new JwtBuilder();
    }

    public static final class Builder {

        private final String name;
        private Jwt jwt;
        private Oauth2Introspection oauth2Introspection;
        private String principalClaim;

        public Builder(String name) {
            this.name = name;
        }

        public Builder withJwt(Jwt jwt) {
            this.jwt = jwt;

            return this;
        }

        public Builder withOauth2Introspection(Oauth2Introspection oauth2Introspection) {
            this.oauth2Introspection = oauth2Introspection;

            return this;
        }

        public Builder withPrincipalClaim(String principalClaim) {
            this.principalClaim = principalClaim;

            return this;
        }

        public TokenRealm build() {
            return new TokenRealm(name, jwt, oauth2Introspection, principalClaim);
        }
    }

    public static final class Jwt {

        private final List<String> issuer;
        private final List<String> audience;
        private final String publicKey;
        private final String keyStore;
        private final String certificate;

        private Jwt(JwtBuilder builder) {
            this.issuer = builder.issuer;
            this.audience = builder.audience;
            this.publicKey = builder.publicKey;
            this.keyStore = builder.keyStore;
            this.certificate = builder.certificate;
        }

        public List<String> getIssuer() {
            return issuer;
        }

        public List<String> getAudience() {
            return audience;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public String getKeyStore() {
            return keyStore;
        }

        public String getCertificate() {
            return certificate;
        }

    }

    public static final class JwtBuilder {

        private List<String> issuer;
        private List<String> audience;
        private String publicKey;
        private String keyStore;
        private String certificate;

        public JwtBuilder withIssuer(String... issuer) {
            if (this.issuer == null) {
                this.issuer = new ArrayList<String>();
            }
            Collections.addAll(this.issuer, issuer);
            return this;
        }

        public JwtBuilder withAudience(String... audience) {
            if (this.audience == null) {
                this.audience = new ArrayList<String>();
            }

            Collections.addAll(this.audience, audience);
            return this;
        }

        public JwtBuilder withPublicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public JwtBuilder withKeyStore(String keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        public JwtBuilder withCertificate(String certificate) {
            this.certificate = certificate;
            return this;
        }

        public Jwt build() {
            return new Jwt(this);
        }
    }

    public static final class Oauth2Introspection {

        private final String clientId;
        private final String clientSecret;
        private final String introspectionUrl;
        private final String clientSslContext;
        private final String hostNameVerificationPolicy;

        private Oauth2Introspection(Oauth2IntrospectionBuilder builder) {
            this.clientId = builder.clientId;
            this.clientSecret = builder.clientSecret;
            this.introspectionUrl = builder.introspectionUrl;
            this.clientSslContext = builder.clientSslContext;
            this.hostNameVerificationPolicy = builder.hostNameVerificationPolicy;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public String getIntrospectionUrl() {
            return introspectionUrl;
        }

        public String getClientSslContext() {
            return clientSslContext;
        }

        public String getHostNameVerificationPolicy() {
            return hostNameVerificationPolicy;
        }
    }

    public static final class Oauth2IntrospectionBuilder {

        private String clientId;
        private String clientSecret;
        private String introspectionUrl;
        private String clientSslContext;
        private String hostNameVerificationPolicy;

        public Oauth2IntrospectionBuilder withClientId(String clientId) {
            this.clientId = clientId;

            return this;
        }

        public Oauth2IntrospectionBuilder withClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;

            return this;
        }

        public Oauth2IntrospectionBuilder withIntrospectionUrl(String introspectionUrl) {
            this.introspectionUrl = introspectionUrl;

            return this;
        }

        public Oauth2IntrospectionBuilder withClientSslContext(String clientSslContext) {
            this.clientSslContext = clientSslContext;

            return this;
        }

        public Oauth2IntrospectionBuilder withHostNameVerificationPolicy(String hostNameVerificationPolicy) {
            this.hostNameVerificationPolicy = hostNameVerificationPolicy;

            return this;
        }

        public Oauth2Introspection build() {
            return new Oauth2Introspection(this);
        }
    }
}
