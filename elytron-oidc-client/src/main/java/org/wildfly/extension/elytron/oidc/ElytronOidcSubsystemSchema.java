/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemRegistrar.SIMPLE_ATTRIBUTE_MARSHALLER;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemRegistrar.SIMPLE_ATTRIBUTE_PARSER;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.ADAPTER_STATE_COOKIE_PATH;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.BEARER_ONLY;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.CLIENT_ID;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.ENABLE_BASIC_AUTH;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.MIN_TIME_BETWEEN_JWKS_REQUESTS;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.PUBLIC_CLIENT;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.PUBLIC_KEY_CACHE_TTL;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.RESOURCE;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.SCOPE;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.TOKEN_MINIMUM_TIME_TO_LIVE;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.USE_RESOURCE_ROLE_MAPPINGS;

import java.util.EnumSet;
import java.util.Map;

import org.jboss.as.controller.Feature;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerated the schema versions for the elytron-oidc-client subsystem.
 * @author Prarthona Paul
 */

public enum ElytronOidcSubsystemSchema implements PersistentSubsystemSchema<ElytronOidcSubsystemSchema> {

    VERSION_1_0(1, Stability.DEFAULT),
    VERSION_2_0(2, Stability.DEFAULT),
    VERSION_2_0_PREVIEW(2, 0, Stability.PREVIEW),
    ;

    static final Map<Stability, ElytronOidcSubsystemSchema> CURRENT = Feature.map(EnumSet.of(VERSION_2_0_PREVIEW, VERSION_2_0));

    private final VersionedNamespace<IntVersion, ElytronOidcSubsystemSchema> namespace;

    ElytronOidcSubsystemSchema(int major, Stability stability) {
        this.namespace = SubsystemSchema.createSubsystemURN(ElytronOidcExtension.SUBSYSTEM_NAME, stability, new IntVersion(major));
    }

    ElytronOidcSubsystemSchema(int major, int minor, Stability stability) {
        this.namespace = SubsystemSchema.createSubsystemURN(ElytronOidcExtension.SUBSYSTEM_NAME, stability, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, ElytronOidcSubsystemSchema> getNamespace()  {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        PersistentResourceXMLDescription.Factory factory = PersistentResourceXMLDescription.factory(this);
        return factory.builder(ElytronOidcClientSubsystemRegistrar.PATH)
                .addChild(factory.builder(RealmDefinition.PATH)
                        .addAttribute(ProviderAttributeDefinitions.ALLOW_ANY_HOSTNAME, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.ALWAYS_REFRESH_TOKEN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.AUTH_SERVER_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.AUTODETECT_BEARER_ONLY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEY_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEYSTORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEYSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONFIDENTIAL_PORT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_POOL_SIZE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_TIMEOUT_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_TTL_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_ALLOWED_HEADERS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_ALLOWED_METHODS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_EXPOSED_HEADERS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_MAX_AGE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.DISABLE_TRUST_MANAGER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.ENABLE_CORS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.EXPOSE_TOKEN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.IGNORE_OAUTH_QUERY_PARAMETER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PRINCIPAL_ATTRIBUTE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PROVIDER_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PROXY_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REALM_PUBLIC_KEY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REGISTER_NODE_AT_STARTUP, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REGISTER_NODE_PERIOD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.SOCKET_TIMEOUT_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.SSL_REQUIRED, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TOKEN_SIGNATURE_ALGORITHM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TOKEN_STORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TRUSTSTORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TRUSTSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.VERIFY_TOKEN_AUDIENCE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .build())
                .addChild(factory.builder(ProviderDefinition.PATH)
                        .addAttribute(ProviderAttributeDefinitions.ALLOW_ANY_HOSTNAME, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.ALWAYS_REFRESH_TOKEN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.AUTH_SERVER_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.AUTODETECT_BEARER_ONLY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEY_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEYSTORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEYSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONFIDENTIAL_PORT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_POOL_SIZE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_TIMEOUT_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_TTL_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_ALLOWED_HEADERS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_ALLOWED_METHODS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_EXPOSED_HEADERS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_MAX_AGE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.DISABLE_TRUST_MANAGER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.ENABLE_CORS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.EXPOSE_TOKEN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.IGNORE_OAUTH_QUERY_PARAMETER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PRINCIPAL_ATTRIBUTE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PROVIDER_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PROXY_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REALM_PUBLIC_KEY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REGISTER_NODE_AT_STARTUP, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REGISTER_NODE_PERIOD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.SOCKET_TIMEOUT_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.SSL_REQUIRED, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TOKEN_SIGNATURE_ALGORITHM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TOKEN_STORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TRUSTSTORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TRUSTSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.VERIFY_TOKEN_AUDIENCE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .build())
                .addChild(factory.builder(SecureDeploymentDefinition.PATH)
                        .addAttribute(ADAPTER_STATE_COOKIE_PATH, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.ALLOW_ANY_HOSTNAME, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.ALWAYS_REFRESH_TOKEN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.AUTH_SERVER_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.AUTODETECT_BEARER_ONLY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(BEARER_ONLY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(CLIENT_ID, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEY_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEYSTORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEYSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONFIDENTIAL_PORT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_POOL_SIZE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_TIMEOUT_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_TTL_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_ALLOWED_HEADERS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_ALLOWED_METHODS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_EXPOSED_HEADERS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_MAX_AGE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.DISABLE_TRUST_MANAGER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ENABLE_BASIC_AUTH, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.ENABLE_CORS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.EXPOSE_TOKEN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.IGNORE_OAUTH_QUERY_PARAMETER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(MIN_TIME_BETWEEN_JWKS_REQUESTS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PRINCIPAL_ATTRIBUTE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(SecureDeploymentDefinition.PROVIDER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PROVIDER_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PROXY_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(PUBLIC_CLIENT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(PUBLIC_KEY_CACHE_TTL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(SecureDeploymentDefinition.REALM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REALM_PUBLIC_KEY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REGISTER_NODE_AT_STARTUP, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REGISTER_NODE_PERIOD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(RESOURCE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(SCOPE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.SOCKET_TIMEOUT_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.SSL_REQUIRED, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(TOKEN_MINIMUM_TIME_TO_LIVE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TOKEN_SIGNATURE_ALGORITHM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TOKEN_STORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TRUSTSTORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TRUSTSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(USE_RESOURCE_ROLE_MAPPINGS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.VERIFY_TOKEN_AUDIENCE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addChild(factory.builder(CredentialDefinition.PATH)
                                .addAttribute(CredentialDefinition.SECRET, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.CLIENT_KEYSTORE_FILE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.CLIENT_KEYSTORE_TYPE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.CLIENT_KEYSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.CLIENT_KEY_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.TOKEN_TIMEOUT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.CLIENT_KEY_ALIAS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.ALGORITHM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .build())
                        .addChild(factory.builder(RedirectRewriteRuleDefinition.PATH)
                                .addAttribute(RedirectRewriteRuleDefinition.REPLACEMENT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .build())
                        .build())
                .addChild(factory.builder(SecureServerDefinition.PATH)
                        .addAttribute(ADAPTER_STATE_COOKIE_PATH, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.ALLOW_ANY_HOSTNAME, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.ALWAYS_REFRESH_TOKEN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.AUTH_SERVER_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.AUTODETECT_BEARER_ONLY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(BEARER_ONLY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(CLIENT_ID, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEY_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEYSTORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CLIENT_KEYSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONFIDENTIAL_PORT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_POOL_SIZE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_TIMEOUT_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CONNECTION_TTL_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_ALLOWED_HEADERS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_ALLOWED_METHODS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_EXPOSED_HEADERS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.CORS_MAX_AGE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.DISABLE_TRUST_MANAGER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ENABLE_BASIC_AUTH, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.ENABLE_CORS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.EXPOSE_TOKEN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.IGNORE_OAUTH_QUERY_PARAMETER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(MIN_TIME_BETWEEN_JWKS_REQUESTS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PRINCIPAL_ATTRIBUTE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(SecureDeploymentDefinition.PROVIDER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PROVIDER_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.PROXY_URL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(PUBLIC_CLIENT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(PUBLIC_KEY_CACHE_TTL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(SecureDeploymentDefinition.REALM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REALM_PUBLIC_KEY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REGISTER_NODE_AT_STARTUP, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.REGISTER_NODE_PERIOD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(RESOURCE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(SCOPE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.SOCKET_TIMEOUT_MILLIS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.SSL_REQUIRED, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(TOKEN_MINIMUM_TIME_TO_LIVE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TOKEN_SIGNATURE_ALGORITHM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TOKEN_STORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TRUSTSTORE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.TRUSTSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(USE_RESOURCE_ROLE_MAPPINGS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addAttribute(ProviderAttributeDefinitions.VERIFY_TOKEN_AUDIENCE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                        .addChild(factory.builder(CredentialDefinition.PATH)
                                .addAttribute(CredentialDefinition.SECRET, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.CLIENT_KEYSTORE_FILE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.CLIENT_KEYSTORE_TYPE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.CLIENT_KEYSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.CLIENT_KEY_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.TOKEN_TIMEOUT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.CLIENT_KEY_ALIAS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .addAttribute(CredentialDefinition.ALGORITHM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .build())
                        .addChild(factory.builder(RedirectRewriteRuleDefinition.PATH)
                                .addAttribute(RedirectRewriteRuleDefinition.REPLACEMENT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
                                .build())
                        .build())
                .build();
    }
}
