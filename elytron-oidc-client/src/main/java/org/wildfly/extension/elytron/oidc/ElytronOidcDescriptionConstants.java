/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

/**
 * Constants used in the Elytron OpenID Connect subsystem.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
interface ElytronOidcDescriptionConstants {

    String ADAPTER_STATE_COOKIE_PATH = "adapter-state-cookie-path";
    String ALGORITHM = "algorithm";
    String ALLOW_ANY_HOSTNAME = "allow-any-hostname";
    String ALWAYS_REFRESH_TOKEN = "always-refresh-token";
    String AUTH_SERVER_URL = "auth-server-url";
    String AUTHENTICATION_REQUEST_FORMAT = "authentication-request-format";
    String AUTODETECT_BEARER_ONLY = "autodetect-bearer-only";
    String BEARER_ONLY = "bearer-only";
    String CLIENT_ID = "client-id";
    String CLIENT_KEY_ALIAS = "client-key-alias";
    String CLIENT_KEY_PASSWORD = "client-key-password";
    String CLIENT_KEYSTORE = "client-keystore";
    String CLIENT_KEYSTORE_FILE = "client-keystore-file";
    String CLIENT_KEYSTORE_PASSWORD = "client-keystore-password";
    String CLIENT_KEYSTORE_TYPE = "client-keystore-type";
    String CONFIDENTIAL_PORT = "confidential-port";
    String CONNECTION_POOL_SIZE = "connection-pool-size";
    String CONNECTION_TIMEOUT_MILLIS = "connection-timeout-millis";
    String CONNECTION_TTL_MILLIS = "connection-ttl-millis";
    String CORS_MAX_AGE = "cors-max-age";
    String CORS_ALLOWED_HEADERS = "cors-allowed-headers";
    String CORS_ALLOWED_METHODS = "cors-allowed-methods";
    String CORS_EXPOSED_HEADERS = "cors-exposed-headers";
    String CREDENTIAL = "credential";
    String CREDENTIALS = "credentials";
    String DISABLE_TRUST_MANAGER = "disable-trust-manager";
    String ENABLE_BASIC_AUTH = "enable-basic-auth";
    String ENABLE_CORS = "enable-cors";
    String EXPOSE_TOKEN = "expose-token";
    String IGNORE_OAUTH_QUERY_PARAMTER = "ignore-oauth-query-parameter";
    String LOGOUT_CALLBACK_PATH = "logout-callback-path";
    String LOGOUT_PATH = "logout-path";
    String LOGOUT_SESSION_REQUIRED ="logout-session-required";
    String MIN_TIME_BETWEEN_JWKS_REQUESTS = "min-time-between-jwks-requests";
    String OAUTH2 = "oauth2";
    String PATTERN = "pattern";
    String POST_LOGOUT_URI ="post-logout-uri";
    String PRINCIPAL_ATTRIBUTE = "principal-attribute";
    String PROVIDER = "provider";
    String PROVIDER_URL = "provider-url";
    String PROXY_URL = "proxy-url";
    String PUBLIC_CLIENT = "public-client";
    String PUBLIC_KEY_CACHE_TTL = "public-key-cache-ttl";
    String REALM = "realm";
    String REALM_PUBLIC_KEY = "realm-public-key";
    String REDIRECT_REWRITE_RULE = "redirect-rewrite-rule";
    String REGISTER_NODE_AT_STARTUP = "register-node-at-startup";
    String REGISTER_NODE_PERIOD = "register-node-period";
    String REPLACEMENT = "replacement";
    String REQUEST = "request";
    String REQUEST_OBJECT_ENCRYPTION_ENC_VALUE = "request-object-encryption-enc-value";
    String REQUEST_OBJECT_ENCRYPTION_ALG_VALUE = "request-object-encryption-alg-value";
    String REQUEST_OBJECT_SIGNING_ALGORITHM = "request-object-signing-algorithm";
    String REQUEST_OBJECT_SIGNING_KEYSTORE_FILE = "request-object-signing-keystore-file";
    String REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD = "request-object-signing-keystore-password";
    String REQUEST_OBJECT_SIGNING_KEY_PASSWORD = "request-object-signing-key-password";
    String REQUEST_OBJECT_SIGNING_KEY_ALIAS = "request-object-signing-key-alias";
    String REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE = "request-object-signing-keystore-type";
    String REQUEST_URI = "request_uri";
    String RESOURCE = "resource";
    String SCOPE = "scope";
    String SECRET = "secret";
    String SECURE_DEPLOYMENT = "secure-deployment";
    String SECURE_SERVER = "secure-server";
    String SOCKET_TIMEOUT_MILLIS = "socket-timeout-millis";
    String SSL_REQUIRED = "ssl-required";
    String TOKEN_MINIMUM_TIME_TO_LIVE = "token-minimum-time-to-live";
    String TOKEN_SIGNATURE_ALGORITHM = "token-signature-algorithm";
    String TOKEN_STORE = "token-store";
    String TOKEN_TIMEOUT = "token-timeout";
    String TRUSTSTORE = "truststore";
    String TRUSTORE_PASSWORD = "truststore-password";
    String TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN = "turn-off-change-session-id-on-login";
    String USE_RESOURCE_ROLE_MAPPINGS = "use-resource-role-mappings";
    String VERIFY_TOKEN_AUDIENCE = "verify-token-audience";

}