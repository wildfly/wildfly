/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.jose4j.jws.AlgorithmIdentifiers.NONE;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.OAUTH2;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REQUEST;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REQUEST_URI;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.security.http.oidc.Oidc;


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

    protected static final SimpleAttributeDefinition PROVIDER_JWT_CLAIMS_TYP =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.PROVIDER_JWT_CLAIMS_TYP, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .setStability(Stability.PREVIEW)
                    .build();

    protected static final SimpleAttributeDefinition SSL_REQUIRED =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.SSL_REQUIRED, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode("external"))
                    .setValidator(EnumValidator.create(Oidc.SSLRequired.class, EnumSet.allOf(Oidc.SSLRequired.class)))
                    .build();

    protected static final SimpleAttributeDefinition ALLOW_ANY_HOSTNAME =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.ALLOW_ANY_HOSTNAME, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    protected static final SimpleAttributeDefinition DISABLE_TRUST_MANAGER =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.DISABLE_TRUST_MANAGER, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
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
                    .setDefaultValue(ModelNode.FALSE)
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
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    protected static final SimpleAttributeDefinition ALWAYS_REFRESH_TOKEN =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.ALWAYS_REFRESH_TOKEN, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    protected static final SimpleAttributeDefinition REGISTER_NODE_AT_STARTUP =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REGISTER_NODE_AT_STARTUP, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
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
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    protected static final SimpleAttributeDefinition IGNORE_OAUTH_QUERY_PARAMETER =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.IGNORE_OAUTH_QUERY_PARAMTER, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
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
                    .setDefaultValue(ModelNode.FALSE)
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

    protected static final SimpleAttributeDefinition AUTHENTICATION_REQUEST_FORMAT =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(OAUTH2))
                    .setValidator(new StringAllowedValuesValidator(OAUTH2, REQUEST, REQUEST_URI))
                    .setStability(Stability.PREVIEW)
                    .build();

    protected static final SimpleAttributeDefinition REQUEST_OBJECT_ENCRYPTION_ALG_VALUE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setRequires(ElytronOidcDescriptionConstants.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE)
                    .setStability(Stability.PREVIEW)
                    .build();

    protected static final SimpleAttributeDefinition REQUEST_OBJECT_ENCRYPTION_ENC_VALUE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setRequires(ElytronOidcDescriptionConstants.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE)
                    .setStability(Stability.PREVIEW)
                    .build();

    protected static final SimpleAttributeDefinition REQUEST_OBJECT_SIGNING_ALGORITHM =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_ALGORITHM, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(NONE)) // plaintext jwt to be sent
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setStability(Stability.PREVIEW)
                    .build();

    protected static final SimpleAttributeDefinition REQUEST_OBJECT_SIGNING_KEYSTORE_FILE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setStability(Stability.PREVIEW)
                    .build();

    protected static final SimpleAttributeDefinition REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setStability(Stability.PREVIEW)
                    .build();

    protected static final SimpleAttributeDefinition REQUEST_OBJECT_SIGNING_KEY_PASSWORD =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEY_PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setStability(Stability.PREVIEW)
                    .build();

    protected static final SimpleAttributeDefinition REQUEST_OBJECT_SIGNING_KEY_ALIAS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEY_ALIAS, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setStability(Stability.PREVIEW)
                    .build();

    protected static final SimpleAttributeDefinition REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setStability(Stability.PREVIEW)
                    .build();


    protected static final SimpleAttributeDefinition[] ATTRIBUTES = { REALM_PUBLIC_KEY, AUTH_SERVER_URL, PROVIDER_URL,
            TRUSTSTORE, TRUSTSTORE_PASSWORD, SSL_REQUIRED, CONFIDENTIAL_PORT, ALLOW_ANY_HOSTNAME, DISABLE_TRUST_MANAGER,
            CONNECTION_POOL_SIZE, ENABLE_CORS, CLIENT_KEYSTORE, CLIENT_KEYSTORE_PASSWORD, CLIENT_KEY_PASSWORD,
            CORS_MAX_AGE, CORS_ALLOWED_HEADERS, CORS_ALLOWED_METHODS, CORS_EXPOSED_HEADERS, EXPOSE_TOKEN,
            ALWAYS_REFRESH_TOKEN, REGISTER_NODE_AT_STARTUP, REGISTER_NODE_PERIOD, TOKEN_STORE, PRINCIPAL_ATTRIBUTE,
            AUTODETECT_BEARER_ONLY, IGNORE_OAUTH_QUERY_PARAMETER, PROXY_URL, VERIFY_TOKEN_AUDIENCE, SOCKET_TIMEOUT_MILLIS,
            CONNECTION_TTL_MILLIS, CONNECTION_TIMEOUT_MILLIS, TOKEN_SIGNATURE_ALGORITHM, AUTHENTICATION_REQUEST_FORMAT,
            REQUEST_OBJECT_SIGNING_ALGORITHM, REQUEST_OBJECT_ENCRYPTION_ENC_VALUE, REQUEST_OBJECT_ENCRYPTION_ALG_VALUE,
            REQUEST_OBJECT_SIGNING_KEYSTORE_FILE, REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD,
            REQUEST_OBJECT_SIGNING_KEY_ALIAS, REQUEST_OBJECT_SIGNING_KEY_PASSWORD, REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE};

    protected static final SimpleAttributeDefinition[] ATTRIBUTES_VERSION_4_0 = addElement(ATTRIBUTES, PROVIDER_JWT_CLAIMS_TYP);

    private static <T> T[] addElement(T[] array, T element) {
        Function<T[], T[]> addAndCreate = arr -> {
            T[] newArray = Arrays.copyOf(arr, arr.length + 1);
            newArray[newArray.length - 1] = element;
            return newArray;
        };
        return addAndCreate.apply(array);
    }
}
