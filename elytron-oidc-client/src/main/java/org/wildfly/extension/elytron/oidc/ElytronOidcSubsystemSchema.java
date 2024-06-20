/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

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
    ;

    static final Map<Stability, ElytronOidcSubsystemSchema> CURRENT = Feature.map(EnumSet.of(VERSION_3_0_PREVIEW, VERSION_2_0));
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

        redirectRewriteRuleDefinitionBuilder.addAttribute(RedirectRewriteRuleDefinition.REPLACEMENT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER);
        Stream.of(CredentialDefinition.ATTRIBUTES).forEach(attribute -> credentialDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(ProviderAttributeDefinitions.ATTRIBUTES).forEach(attribute -> realmDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(ProviderAttributeDefinitions.ATTRIBUTES).forEach(attribute -> providerDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(secureDeploymentDefaultAttributes).forEach(attribute -> secureDeploymentDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(secureDeploymentDefaultAttributes).forEach(attribute -> secureServerDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(ProviderAttributeDefinitions.ATTRIBUTES).forEach(attribute -> secureDeploymentDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));
        Stream.of(ProviderAttributeDefinitions.ATTRIBUTES).forEach(attribute -> secureServerDefinitionBuilder.addAttribute(attribute, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER));

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
