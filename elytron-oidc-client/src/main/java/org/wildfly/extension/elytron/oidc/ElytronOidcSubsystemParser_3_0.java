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

import org.jboss.as.controller.PersistentResourceXMLDescription;

/**
 * Subsystem parser for the Elytron OpenID Connect subsystem.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class ElytronOidcSubsystemParser_3_0 extends ElytronOidcSubsystemParser_2_0 {

    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE_3_0 = "urn:wildfly:elytron-oidc-client:3.0";

    @Override
    String getNameSpace() {
        return NAMESPACE_3_0;
    }

    final PersistentResourceXMLDescription secureDeploymentParser = secureDeploymentParserBuilder
            .addAttribute(SecureDeploymentDefinition.REALM, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(SecureDeploymentDefinition.PROVIDER, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(RESOURCE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(CLIENT_ID, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(USE_RESOURCE_ROLE_MAPPINGS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(BEARER_ONLY, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(ENABLE_BASIC_AUTH, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(PUBLIC_CLIENT, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(TOKEN_MINIMUM_TIME_TO_LIVE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(MIN_TIME_BETWEEN_JWKS_REQUESTS, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(PUBLIC_KEY_CACHE_TTL, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(ADAPTER_STATE_COOKIE_PATH, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER)
            .addAttribute(SCOPE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER) // new in 3.0
            .addChild(redirectRewriteRuleParser)
            .addChild(credentialParser)
            .setUseElementsForGroups(true)
            .build();

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
            .addAttribute(SCOPE, SIMPLE_ATTRIBUTE_PARSER, SIMPLE_ATTRIBUTE_MARSHALLER) //new in 3.0
            .addChild(redirectRewriteRuleParser)
            .addChild(credentialParser)
            .setUseElementsForGroups(true)
            .build();

    @Override
    public PersistentResourceXMLDescription getSecureDeploymentParser() {
        return secureDeploymentParser;
    }

    @Override
    public PersistentResourceXMLDescription getSecureServerParser() {
        return secureServerParser;
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return PersistentResourceXMLDescription.builder(ElytronOidcExtension.SUBSYSTEM_PATH, getNameSpace())
                .addChild(getRealmParser())
                .addChild(getProviderParser())
                .addChild(getSecureDeploymentParser()) //new scope attribute in 3.0
                .addChild(getSecureServerParser()) //new scope attribute in 3.0
                .build();
    }
}
