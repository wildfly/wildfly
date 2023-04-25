/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_SERVER;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.ALLOW_ANY_HOSTNAME;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.ALWAYS_REFRESH_TOKEN;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.AUTH_SERVER_URL;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.AUTODETECT_BEARER_ONLY;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CLIENT_KEYSTORE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CLIENT_KEYSTORE_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CLIENT_KEY_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CONFIDENTIAL_PORT;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CONNECTION_POOL_SIZE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CONNECTION_TIMEOUT_MILLIS;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CONNECTION_TTL_MILLIS;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CORS_ALLOWED_HEADERS;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CORS_ALLOWED_METHODS;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CORS_EXPOSED_HEADERS;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CORS_MAX_AGE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.DISABLE_TRUST_MANAGER;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.ENABLE_CORS;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.EXPOSE_TOKEN;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.IGNORE_OAUTH_QUERY_PARAMETER;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.PRINCIPAL_ATTRIBUTE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.PROVIDER_URL;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.PROXY_URL;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REALM_PUBLIC_KEY;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REGISTER_NODE_AT_STARTUP;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REGISTER_NODE_PERIOD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.SOCKET_TIMEOUT_MILLIS;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.SSL_REQUIRED;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.TOKEN_SIGNATURE_ALGORITHM;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.TOKEN_STORE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.TRUSTSTORE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.TRUSTSTORE_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.VERIFY_TOKEN_AUDIENCE;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.ADAPTER_STATE_COOKIE_PATH;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.BEARER_ONLY;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.CLIENT_ID;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.ENABLE_BASIC_AUTH;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.MIN_TIME_BETWEEN_JWKS_REQUESTS;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.PUBLIC_CLIENT;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.PUBLIC_KEY_CACHE_TTL;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.RESOURCE;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.TOKEN_MINIMUM_TIME_TO_LIVE;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.USE_RESOURCE_ROLE_MAPPINGS;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;

/**
 * Subsystem parser for the Elytron OpenID Connect subsystem.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class ElytronOidcSubsystemParser_2_0 extends ElytronOidcSubsystemParser_1_0 {

    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE_2_0 = "urn:wildfly:elytron-oidc-client:2.0";

    @Override
    String getNameSpace() {
        return NAMESPACE_2_0;
    }

    final PersistentResourceXMLDescription.PersistentResourceXMLBuilder secureServerParserBuilder = PersistentResourceXMLDescription.builder(PathElement.pathElement(SECURE_SERVER))
            .addAttribute(REALM_PUBLIC_KEY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(AUTH_SERVER_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(PROVIDER_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(TRUSTSTORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(TRUSTSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(SSL_REQUIRED, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CONFIDENTIAL_PORT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(ALLOW_ANY_HOSTNAME, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(DISABLE_TRUST_MANAGER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CONNECTION_POOL_SIZE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(ENABLE_CORS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CLIENT_KEYSTORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CLIENT_KEYSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CLIENT_KEY_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CORS_MAX_AGE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CORS_ALLOWED_HEADERS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CORS_ALLOWED_METHODS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CORS_EXPOSED_HEADERS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(EXPOSE_TOKEN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(ALWAYS_REFRESH_TOKEN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(REGISTER_NODE_AT_STARTUP, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(REGISTER_NODE_PERIOD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(TOKEN_STORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(PRINCIPAL_ATTRIBUTE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(AUTODETECT_BEARER_ONLY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(IGNORE_OAUTH_QUERY_PARAMETER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(PROXY_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(VERIFY_TOKEN_AUDIENCE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(SOCKET_TIMEOUT_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CONNECTION_TTL_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CONNECTION_TIMEOUT_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(TOKEN_SIGNATURE_ALGORITHM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER);

    final PersistentResourceXMLDescription secureServerParser = secureServerParserBuilder
            .addAttribute(SecureDeploymentDefinition.REALM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(SecureDeploymentDefinition.PROVIDER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(RESOURCE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(CLIENT_ID, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(USE_RESOURCE_ROLE_MAPPINGS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(BEARER_ONLY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(ENABLE_BASIC_AUTH, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(PUBLIC_CLIENT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(TOKEN_MINIMUM_TIME_TO_LIVE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(MIN_TIME_BETWEEN_JWKS_REQUESTS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(PUBLIC_KEY_CACHE_TTL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(ADAPTER_STATE_COOKIE_PATH, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addChild(redirectRewriteRuleParser)
            .addChild(credentialParser)
            .setUseElementsForGroups(true)
            .build();

    PersistentResourceXMLDescription getSecureServerParser() {
        return secureServerParser;
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return PersistentResourceXMLDescription.builder(ElytronOidcExtension.SUBSYSTEM_PATH, getNameSpace())
                .addChild(getRealmParser())
                .addChild(getProviderParser())
                .addChild(getSecureDeploymentParser())
                .addChild(getSecureServerParser()) // new in 2.0
                .build();
    }
}

