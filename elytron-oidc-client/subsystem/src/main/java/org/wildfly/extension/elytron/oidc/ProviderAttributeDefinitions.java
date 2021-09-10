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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * The common attribute definitions for OpenID Connect provider attributes.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class ProviderAttributeDefinitions {

    protected static final SimpleAttributeDefinition REALM_PUBLIC_KEY =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REALM_PUBLIC_KEY, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition AUTH_SERVER_URL =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.AUTH_SERVER_URL, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setAlternatives(ElytronOidcDescriptionConstants.PROVIDER_URL)
                    .build();

    protected static final SimpleAttributeDefinition PROVIDER_URL =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.PROVIDER_URL, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setAlternatives(ElytronOidcDescriptionConstants.AUTH_SERVER_URL)
                    .build();

    protected static final SimpleAttributeDefinition SSL_REQUIRED =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.SSL_REQUIRED, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode("external"))
                    .setAllowedValues("external", "all", "none")
                    .build();

    protected static final SimpleAttributeDefinition ALLOW_ANY_HOSTNAME =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.ALLOW_ANY_HOSTNAME, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition DISABLE_TRUST_MANAGER =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.DISABLE_TRUST_MANAGER, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition TRUSTSTORE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.TRUSTSTORE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition TRUSTSTORE_PASSWORD =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.TRUSTORE_PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition CONNECTION_POOL_SIZE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CONNECTION_POOL_SIZE, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(0, true))
                    .build();

    protected static final SimpleAttributeDefinition ENABLE_CORS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.ENABLE_CORS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition CLIENT_KEYSTORE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CLIENT_KEYSTORE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition CLIENT_KEYSTORE_PASSWORD =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CLIENT_KEYSTORE_PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition CLIENT_KEY_PASSWORD =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CLIENT_KEY_PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition CORS_MAX_AGE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CORS_MAX_AGE, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .build();

    protected static final SimpleAttributeDefinition CORS_ALLOWED_HEADERS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CORS_ALLOWED_HEADERS, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition CORS_ALLOWED_METHODS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CORS_ALLOWED_METHODS, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition CORS_EXPOSED_HEADERS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CORS_EXPOSED_HEADERS, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition EXPOSE_TOKEN =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.EXPOSE_TOKEN, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition ALWAYS_REFRESH_TOKEN =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.ALWAYS_REFRESH_TOKEN, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition REGISTER_NODE_AT_STARTUP =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REGISTER_NODE_AT_STARTUP, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition REGISTER_NODE_PERIOD =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REGISTER_NODE_PERIOD, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .build();

    protected static final SimpleAttributeDefinition TOKEN_STORE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.TOKEN_STORE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition PRINCIPAL_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.PRINCIPAL_ATTRIBUTE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition AUTODETECT_BEARER_ONLY =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.AUTODETECT_BEARER_ONLY, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition IGNORE_OAUTH_QUERY_PARAMETER =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.IGNORE_OAUTH_QUERY_PARAMTER, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition CONFIDENTIAL_PORT =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CONFIDENTIAL_PORT, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(8443))
                    .build();

    protected static final SimpleAttributeDefinition PROXY_URL =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.PROXY_URL, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition VERIFY_TOKEN_AUDIENCE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.VERIFY_TOKEN_AUDIENCE, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition SOCKET_TIMEOUT_MILLIS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.SOCKET_TIMEOUT_MILLIS, ModelType.LONG, true)
                    .setAllowExpression(true)
                    .setValidator(new LongRangeValidator(-1L, true))
                    .build();
    protected static final SimpleAttributeDefinition CONNECTION_TTL_MILLIS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CONNECTION_TTL_MILLIS, ModelType.LONG, true)
                    .setAllowExpression(true)
                    .setValidator(new LongRangeValidator(-1L, true))
                    .build();
    protected static final SimpleAttributeDefinition CONNECTION_TIMEOUT_MILLIS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CONNECTION_TIMEOUT_MILLIS, ModelType.LONG, true)
                    .setAllowExpression(true)
                    .setValidator(new LongRangeValidator(-1L, true))
                    .build();

    protected static final SimpleAttributeDefinition TOKEN_SIGNATURE_ALGORITHM =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.TOKEN_SIGNATURE_ALGORITHM, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode("RS256"))
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition[] ATTRIBUTES = { REALM_PUBLIC_KEY, AUTH_SERVER_URL, PROVIDER_URL, TRUSTSTORE, TRUSTSTORE_PASSWORD,
            SSL_REQUIRED, CONFIDENTIAL_PORT, ALLOW_ANY_HOSTNAME, DISABLE_TRUST_MANAGER, CONNECTION_POOL_SIZE, ENABLE_CORS, CLIENT_KEYSTORE,
            CLIENT_KEYSTORE_PASSWORD, CLIENT_KEY_PASSWORD, CORS_MAX_AGE, CORS_ALLOWED_HEADERS, CORS_ALLOWED_METHODS, CORS_EXPOSED_HEADERS,
            EXPOSE_TOKEN, ALWAYS_REFRESH_TOKEN, REGISTER_NODE_AT_STARTUP, REGISTER_NODE_PERIOD, TOKEN_STORE, PRINCIPAL_ATTRIBUTE, AUTODETECT_BEARER_ONLY,
            IGNORE_OAUTH_QUERY_PARAMETER, PROXY_URL, VERIFY_TOKEN_AUDIENCE, SOCKET_TIMEOUT_MILLIS, CONNECTION_TTL_MILLIS, CONNECTION_TIMEOUT_MILLIS, TOKEN_SIGNATURE_ALGORITHM};

}