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
    String MIN_TIME_BETWEEN_JWKS_REQUESTS = "min-time-between-jwks-requests";
    String PATTERN = "pattern";
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
    String RESOURCE = "resource";
    String SECRET = "secret";
    String SECURE_DEPLOYMENT = "secure-deployment";
    String SECURE_SERVER = "secure-server";
    String SOCKET_TIMEOUT_MILLIS = "socket-timeout-millis";
    String SSL_REQUIRED = "ssl-required";
    String TOKEN_MINIMUM_TIME_TO_LIVE = "token-minimum-time-to-live";
    String TOKEN_STORE = "token-store";
    String TOKEN_TIMEOUT = "token-timeout";
    String TRUSTSTORE = "truststore";
    String TRUSTORE_PASSWORD = "truststore-password";
    String TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN = "turn-off-change-session-id-on-login";
    String USE_RESOURCE_ROLE_MAPPINGS = "use-resource-role-mappings";
    String VERIFY_TOKEN_AUDIENCE = "verify-token-audience";

}