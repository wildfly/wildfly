/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
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

import static org.wildfly.extension.elytron.oidc.CredentialDefinition.ALGORITHM;
import static org.wildfly.extension.elytron.oidc.CredentialDefinition.CLIENT_KEYSTORE_FILE;
import static org.wildfly.extension.elytron.oidc.CredentialDefinition.CLIENT_KEYSTORE_TYPE;
import static org.wildfly.extension.elytron.oidc.CredentialDefinition.CLIENT_KEY_ALIAS;
import static org.wildfly.extension.elytron.oidc.CredentialDefinition.SECRET;
import static org.wildfly.extension.elytron.oidc.CredentialDefinition.TOKEN_TIMEOUT;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.CREDENTIAL;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.PROVIDER;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REALM;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REDIRECT_REWRITE_RULE;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT;
import static org.wildfly.extension.elytron.oidc.RedirectRewriteRuleDefinition.REPLACEMENT;
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

import java.util.Collections;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Subsystem parser for the Elytron OpenID Connect subsystem.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class ElytronOidcSubsystemParser_1_0 extends PersistentResourceXMLParser {

    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE = "urn:wildfly:elytron-oidc-client:1.0";

    static final AttributeParser SIMPLE_ATTRIBUTE_PARSER = new AttributeElementParser();
    static final AttributeMarshaller SIMPLE_ATTRIBUTE_MARSHALLER = new AttributeElementMarshaller();

    static PersistentResourceXMLDescription.PersistentResourceXMLBuilder realmParserBuilder = PersistentResourceXMLDescription.builder(PathElement.pathElement(REALM));
    static {
        for (AttributeDefinition attributeDefinition : ProviderAttributeDefinitions.ATTRIBUTES) {
            realmParserBuilder.addAttribute(attributeDefinition, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER );
        }
    }
    final PersistentResourceXMLDescription realmParser = realmParserBuilder.build();

    static PersistentResourceXMLDescription.PersistentResourceXMLBuilder providerParserBuilder = PersistentResourceXMLDescription.builder(PathElement.pathElement(PROVIDER));
    static {
        for (AttributeDefinition attributeDefinition : ProviderAttributeDefinitions.ATTRIBUTES) {
            providerParserBuilder.addAttribute(attributeDefinition, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER );
        }
    }
    final PersistentResourceXMLDescription providerParser = providerParserBuilder.build();

    static PersistentResourceXMLDescription.PersistentResourceXMLBuilder secureDeploymentParserBuilder = PersistentResourceXMLDescription.builder(PathElement.pathElement(SECURE_DEPLOYMENT));
    static {
        for (AttributeDefinition attributeDefinition : ProviderAttributeDefinitions.ATTRIBUTES) {
            secureDeploymentParserBuilder.addAttribute(attributeDefinition, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER );
        }
    }

    final PersistentResourceXMLDescription credentialParser = PersistentResourceXMLDescription.builder(PathElement.pathElement(CREDENTIAL))
            .addAttribute(SECRET)
            .addAttribute(CLIENT_KEYSTORE_FILE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(CLIENT_KEYSTORE_TYPE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(CredentialDefinition.CLIENT_KEYSTORE_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(CredentialDefinition.CLIENT_KEY_PASSWORD, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(TOKEN_TIMEOUT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(CLIENT_KEY_ALIAS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .addAttribute(ALGORITHM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER )
            .build();

    final PersistentResourceXMLDescription redirectRewriteRuleParser = PersistentResourceXMLDescription.builder(PathElement.pathElement(REDIRECT_REWRITE_RULE))
            .addAttribute(REPLACEMENT)
            .build();

    final PersistentResourceXMLDescription secureDeploymentParser = secureDeploymentParserBuilder
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

    PersistentResourceXMLDescription getRealmParser() {
        return realmParser;
    }

    PersistentResourceXMLDescription getProviderParser() {
        return providerParser;
    }

    PersistentResourceXMLDescription getSecureDeploymentParser() {
        return secureDeploymentParser;
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return PersistentResourceXMLDescription.builder(ElytronOidcExtension.SUBSYSTEM_PATH, NAMESPACE)
                .addChild(getRealmParser())
                .addChild(getProviderParser())
                .addChild(getSecureDeploymentParser())
                .build();
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

    static class AttributeElementMarshaller extends AttributeMarshaller.AttributeElementMarshaller {

        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            writer.writeStartElement(attribute.getXmlName());
            marshallElementContent(resourceModel.get(attribute.getName()).asString(), writer);
            writer.writeEndElement();
        }
    }

}

