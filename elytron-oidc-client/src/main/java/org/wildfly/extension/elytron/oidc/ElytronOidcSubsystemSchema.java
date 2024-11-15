/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.ALLOW_ANY_HOSTNAME;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.ALWAYS_REFRESH_TOKEN;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.AUTH_SERVER_URL;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.AUTODETECT_BEARER_ONLY;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CLIENT_KEYSTORE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CLIENT_KEY_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.CLIENT_KEYSTORE_PASSWORD;
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
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE;
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
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.SCOPE;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.PROVIDER;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.REALM;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.TOKEN_MINIMUM_TIME_TO_LIVE;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.USE_RESOURCE_ROLE_MAPPINGS;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.Feature;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Enumerated the schema versions for the elytron-oidc-client subsystem.
 * @author Prarthona Paul
 */
public enum ElytronOidcSubsystemSchema implements PersistentSubsystemSchema<ElytronOidcSubsystemSchema> {

    VERSION_1_0(1, Stability.DEFAULT),
    VERSION_2_0(2, Stability.DEFAULT),
    VERSION_2_0_PREVIEW(2, 0, Stability.PREVIEW), // WildFly 32.0-present
    VERSION_3_0_PREVIEW(3, 0, Stability.PREVIEW), // WildFly 33.0-present
    VERSION_4_0_PREVIEW(4, 0, Stability.PREVIEW), // WildFly 34.0-present
    ;

    static final Map<Stability, ElytronOidcSubsystemSchema> CURRENT = Feature.map(EnumSet.of(VERSION_4_0_PREVIEW, VERSION_2_0));
    private static final AttributeParser SIMPLE_ATTRIBUTE_PARSER = new AttributeElementParser();
    private static final AttributeMarshaller SIMPLE_ATTRIBUTE_MARSHALLER = new AttributeElementMarshaller();

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
        PersistentResourceXMLDescription.Builder elytronOidcClientBuilder =  factory.builder(ElytronOidcSubsystemDefinition.PATH);
        PersistentResourceXMLDescription.Builder realmDefinitionBuilder =  factory.builder(RealmDefinition.PATH);
        PersistentResourceXMLDescription.Builder providerDefinitionBuilder =  factory.builder(ProviderDefinition.PATH);
        PersistentResourceXMLDescription.Builder secureServerDefinitionBuilder =  factory.builder(SecureServerDefinition.PATH);
        PersistentResourceXMLDescription.Builder credentialDefinitionBuilder =  factory.builder(CredentialDefinition.PATH);
        PersistentResourceXMLDescription.Builder redirectRewriteRuleDefinitionBuilder =  factory.builder(RedirectRewriteRuleDefinition.PATH);
        PersistentResourceXMLDescription.Builder secureDeploymentDefinitionBuilder =  factory.builder(SecureDeploymentDefinition.PATH);
        SimpleAttributeDefinition[] secureDeploymentDefaultAttributes = {ADAPTER_STATE_COOKIE_PATH, BEARER_ONLY, CLIENT_ID, ENABLE_BASIC_AUTH, MIN_TIME_BETWEEN_JWKS_REQUESTS,
        PROVIDER, PUBLIC_CLIENT, PUBLIC_KEY_CACHE_TTL, REALM, RESOURCE, TOKEN_MINIMUM_TIME_TO_LIVE, TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN, USE_RESOURCE_ROLE_MAPPINGS};

        SimpleAttributeDefinition[] providerDefaultAttributes = { REALM_PUBLIC_KEY, AUTH_SERVER_URL, PROVIDER_URL,
                TRUSTSTORE, TRUSTSTORE_PASSWORD, SSL_REQUIRED, CONFIDENTIAL_PORT, ALLOW_ANY_HOSTNAME, DISABLE_TRUST_MANAGER,
                CONNECTION_POOL_SIZE, ENABLE_CORS, CLIENT_KEYSTORE, CLIENT_KEYSTORE_PASSWORD, CLIENT_KEY_PASSWORD,
                CORS_MAX_AGE, CORS_ALLOWED_HEADERS, CORS_ALLOWED_METHODS, CORS_EXPOSED_HEADERS, EXPOSE_TOKEN,
                ALWAYS_REFRESH_TOKEN, REGISTER_NODE_AT_STARTUP, REGISTER_NODE_PERIOD, TOKEN_STORE, PRINCIPAL_ATTRIBUTE,
                AUTODETECT_BEARER_ONLY, IGNORE_OAUTH_QUERY_PARAMETER, PROXY_URL, VERIFY_TOKEN_AUDIENCE, SOCKET_TIMEOUT_MILLIS,
                CONNECTION_TTL_MILLIS, CONNECTION_TIMEOUT_MILLIS, TOKEN_SIGNATURE_ALGORITHM};
        SimpleAttributeDefinition[] requestObjectAttributes = {AUTHENTICATION_REQUEST_FORMAT,
                REQUEST_OBJECT_SIGNING_ALGORITHM, REQUEST_OBJECT_ENCRYPTION_ALG_VALUE, REQUEST_OBJECT_ENCRYPTION_ENC_VALUE,
                REQUEST_OBJECT_SIGNING_KEYSTORE_FILE, REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD,
                REQUEST_OBJECT_SIGNING_KEY_ALIAS, REQUEST_OBJECT_SIGNING_KEY_PASSWORD, REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE};

        redirectRewriteRuleDefinitionBuilder.addAttribute(RedirectRewriteRuleDefinition.REPLACEMENT);
        Stream.of(CredentialDefinition.ATTRIBUTES).forEach(attribute -> credentialDefinitionBuilder.addAttribute(attribute));
        Stream.of(providerDefaultAttributes).forEach(attribute -> realmDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(providerDefaultAttributes).forEach(attribute -> providerDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(secureDeploymentDefaultAttributes).forEach(attribute -> secureDeploymentDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(secureDeploymentDefaultAttributes).forEach(attribute -> secureServerDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(providerDefaultAttributes).forEach(attribute -> secureDeploymentDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(providerDefaultAttributes).forEach(attribute -> secureServerDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));

        if (this.since(VERSION_4_0_PREVIEW) && this.enables(AUTHENTICATION_REQUEST_FORMAT)) {
            Stream.of(requestObjectAttributes).forEach(attribute -> secureDeploymentDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
            Stream.of(requestObjectAttributes).forEach(attribute -> secureServerDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
            Stream.of(requestObjectAttributes).forEach(attribute -> realmDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
            Stream.of(requestObjectAttributes).forEach(attribute -> providerDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        }

        if (this.since(VERSION_3_0_PREVIEW) && this.enables(AUTHENTICATION_REQUEST_FORMAT)) {
            Stream.of(requestObjectAttributes).forEach(attribute -> secureDeploymentDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
            Stream.of(requestObjectAttributes).forEach(attribute -> secureServerDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
            Stream.of(requestObjectAttributes).forEach(attribute -> realmDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
            Stream.of(requestObjectAttributes).forEach(attribute -> providerDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        }

        if (this.since(VERSION_2_0_PREVIEW) && this.enables(SCOPE)) {
            secureDeploymentDefinitionBuilder.addAttribute(SCOPE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER);
            secureServerDefinitionBuilder.addAttribute(SCOPE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER);
        }

        elytronOidcClientBuilder
                .addChild(realmDefinitionBuilder.build())
                .addChild(providerDefinitionBuilder.build());

        elytronOidcClientBuilder
                .addChild(secureDeploymentDefinitionBuilder
                        .addChild(credentialDefinitionBuilder.build())
                        .addChild(redirectRewriteRuleDefinitionBuilder.build())
                        .build());

        // Secure-server is supported for version 2.0 and later
        if (this.since(ElytronOidcSubsystemSchema.VERSION_2_0)) {
            elytronOidcClientBuilder
                    .addChild(secureServerDefinitionBuilder
                            .addChild(credentialDefinitionBuilder.build())
                            .addChild(redirectRewriteRuleDefinitionBuilder.build())
                            .build());
        }
        return elytronOidcClientBuilder.build();
    }

    static class AttributeElementMarshaller extends AttributeMarshaller.AttributeElementMarshaller {
        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            writer.writeStartElement(attribute.getXmlName());
            marshallElementContent(resourceModel.get(attribute.getName()).asString(), writer);
            writer.writeEndElement();
        }
    }
    static class AttributeElementParser extends AttributeParser {

        @Override
        public boolean isParseAsElement() {
            return true;
        }

        @Override
        public void parseElement(AttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            assert attribute instanceof SimpleAttributeDefinition;
            if (operation.hasDefined(attribute.getName())) {
                throw ParseUtils.unexpectedElement(reader);
            } else if (attribute.getXmlName().equals(reader.getLocalName())) {
                ((SimpleAttributeDefinition) attribute).parseAndSetParameter(reader.getElementText(), operation, reader);
            } else {
                throw ParseUtils.unexpectedElement(reader, Collections.singleton(attribute.getXmlName()));
            }
        }
    }
}
