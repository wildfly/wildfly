/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client;

import static org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest.MULTIPLE_SCOPE_APP;
import static org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest.SINGLE_SCOPE_APP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.restassured.config.HttpClientConfig;
import org.apache.http.params.CoreConnectionPNames;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.wildfly.security.ssl.test.util.CAGenerationTool;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import javax.security.auth.x500.X500Principal;

/**
 * Keycloak configuration for testing.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class KeycloakConfiguration {

    public static final String USER_ROLE = "user";
    public static final String JBOSS_ADMIN_ROLE = "JBossAdmin";
    public static final String ALICE = "alice";
    public static final String ALICE_PASSWORD = "alice123+";
    public static final String BOB = "bob";
    public static final String BOB_PASSWORD = "bob123+";
    public static final String CHARLIE = "charlie";
    public static final String CHARLIE_PASSWORD = "charlie123+";
    public static final String ALLOWED_ORIGIN = "http://somehost";
    public static final String RSA_KEYSTORE_FILE_NAME = "jwt.keystore";
    public static final String EC_KEYSTORE_FILE_NAME = "jwtEC.keystore";
    public static final String KEYSTORE_ALIAS = "jwtKeystore";
    public static final String KEYSTORE_PASS = "Elytron";
    public static final String KEYSTORE_FILE_NAME = "jwt.keystore";
    public static String KEYSTORE_CLASSPATH;

    public static final String PKCS12_KEYSTORE_TYPE = "PKCS12";

    /* Accepted Request Object Encrypting Algorithms for KeyCloak*/
    public static final String RSA_OAEP = "RSA-OAEP";
    public static final String RSA_OAEP_256 = "RSA-OAEP-256";
    public static final String RSA1_5 = "RSA1_5";

    /* Accepted Request Object Encryption Methods for KeyCloak*/
    public static final String A128CBC_HS256 = "A128CBC-HS256";
    public static final String A192CBC_HS384 = "A192CBC-HS384";
    public static final String A256CBC_HS512 = "A256CBC-HS512";
    public static CAGenerationTool caGenerationTool = null;

    // the users below are for multi-tenancy tests specifically
    public static final String TENANT1_USER = "tenant1_user";
    public static final String TENANT1_PASSWORD = "tenant1_password";
    public static final String TENANT2_USER = "tenant2_user";
    public static final String TENANT2_PASSWORD = "tenant2_password";
    public static final String CHARLOTTE = "charlotte";
    public static final String CHARLOTTE_PASSWORD = " charlotte123+";
    public static final String DAN = "dan";
    public static final String DAN_PASSWORD = " dan123+";
    public static final String TENANT1_REALM = "tenant1";
    public static final String TENANT2_REALM = "tenant2";
    public static final String TENANT1_ENDPOINT = "/tenant1";
    public static final String TENANT2_ENDPOINT = "/tenant2";
    public static final String ALICE_FIRST_NAME = "Alice";
    public static final String ALICE_LAST_NAME = "Smith";
    public static final boolean ALICE_EMAIL_VERIFIED = true;

    public enum ClientAppType {
        OIDC_CLIENT,
        DIRECT_ACCESS_GRANT_OIDC_CLIENT,
        BEARER_ONLY_CLIENT,
        CORS_CLIENT
    }

    /**
     * Configure RealmRepresentation as follows:
     * <ul>
     * <li>Two realm roles ("JBossAdmin", "user")</li>
     * <li>Two users:<li>
     * <ul>
     * <li>user named alice and password alice123+ with "JBossAdmin" and "user" role</li>
     * <li>user named bob and password bob123+ with "user" role</li>
     * </ul>
     * </ul>
     */
    public static RealmRepresentation getRealmRepresentation(final String realmName, String clientSecret,
                                                             String clientHostName, int clientPort, Map<String, ClientAppType> clientApps) throws Exception {
        return createRealm(realmName, clientSecret, clientHostName, clientPort, clientApps, 3, 3, false);
    }

    public static RealmRepresentation getRealmRepresentation(final String realmName, String clientSecret,
                                                             String clientHostName, int clientPort, Map<String, ClientAppType> clientApps,
                                                             int accessTokenLifespan, int ssoSessionMaxLifespan, boolean multiTenancyApp) throws Exception {
        return createRealm(realmName, clientSecret, clientHostName, clientPort, clientApps, accessTokenLifespan, ssoSessionMaxLifespan, multiTenancyApp);
    }

    /**
     * Returns the admin access token from the Keycloak container. Throws an exception if token could not be obtained
     * within the configured timeout of three minutes.
     *
     * @throws Exception exception
     */
    public static String getAdminAccessToken(String authServerUrl) throws Exception {
        RequestSpecification requestSpecification = RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", KeycloakContainer.ADMIN_USER)
                .param("password", KeycloakContainer.ADMIN_PASSWORD)
                .param("client_id", "admin-cli")
                .config(RestAssured.config()
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5_000)
                                .setParam(CoreConnectionPNames.SO_TIMEOUT, 5_000)));


        final long deadline = System.currentTimeMillis() + 180_000L;
        Response response = null;

        while (System.currentTimeMillis() <= deadline) {
            try {
                response = requestSpecification.when().post(authServerUrl + "/realms/master/protocol/openid-connect/token");
            } catch (RuntimeException e) {
                // network/connect error — retry with backoff
                Thread.sleep(1_000);
                continue;
            }

            int status = response.getStatusCode();
            if (status == 200) {
                return response.as(AccessTokenResponse.class).getToken();
            }

            // retry with a backoff interval as not to overload the booting keycloak container with requests
            Thread.sleep(1_000);
        }

        throw new IllegalStateException("Timed out waiting for Keycloak to return an admin token. Last response was: " + (response != null ? response.asPrettyString() : "network/connect error"));
    }

    public static String getAccessToken(String authServerUrl, String realmName, String username, String password, String clientId, String clientSecret) {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", username)
                .param("password", password)
                .param("client_id", clientId)
                .param("client_secret", clientSecret)
                .when()
                .post(authServerUrl + "/realms/" + realmName + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private static RealmRepresentation createRealm(String name, String clientSecret,
                                                   String clientHostName, int clientPort, Map<String, ClientAppType> clientApps) throws Exception {
        return createRealm(name, clientSecret, clientHostName, clientPort, clientApps, 3, 3, false);
    }

    private static RealmRepresentation createRealm(String name, String clientSecret,
                                                   String clientHostName, int clientPort, Map<String, ClientAppType> clientApps,
                                                   int accessTokenLifespan, int ssoSessionMaxLifespan, boolean multiTenancyApp) throws Exception {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(accessTokenLifespan);
        realm.setSsoSessionMaxLifespan(ssoSessionMaxLifespan);

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation(USER_ROLE, null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation(JBOSS_ADMIN_ROLE, null, false));

        for (Map.Entry<String, ClientAppType> entry : clientApps.entrySet()) {
            String clientApp = entry.getKey();
            String multiTenancyRedirectUri = null;
            if (multiTenancyApp) {
                if (name.equals(TENANT1_REALM)) {
                    multiTenancyRedirectUri = "http://" + clientHostName + ":" + clientPort + "/" + clientApp + TENANT1_ENDPOINT;
                } else if (name.equals(TENANT2_REALM)) {
                    multiTenancyRedirectUri = "http://" + clientHostName + ":" + clientPort + "/" + clientApp + TENANT2_ENDPOINT;
                }
            }

            switch (entry.getValue()) {
                case DIRECT_ACCESS_GRANT_OIDC_CLIENT:
                    realm.getClients().add(createWebAppClient(clientApp, clientSecret, clientHostName, clientPort, clientApp, true, multiTenancyRedirectUri));
                    break;
                case BEARER_ONLY_CLIENT:
                    realm.getClients().add(createBearerOnlyClient(clientApp));
                    break;
                case CORS_CLIENT:
                    realm.getClients().add(createWebAppClient(clientApp, clientSecret, clientHostName, clientPort, clientApp, true, ALLOWED_ORIGIN, multiTenancyRedirectUri));
                    break;
                default:
                    realm.getClients().add(createWebAppClient(clientApp, clientSecret, clientHostName, clientPort, clientApp, false, multiTenancyRedirectUri));
            }
        }

        if (name.equals(TENANT1_REALM)) {
            realm.getUsers().add(createUser(TENANT1_USER, TENANT1_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
            realm.getUsers().add(createUser(CHARLOTTE, CHARLOTTE_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
            realm.getUsers().add(createUser(DAN, DAN_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
        } else if (name.equals(TENANT2_REALM)) {
            realm.getUsers().add(createUser(TENANT2_USER, TENANT2_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
            realm.getUsers().add(createUser(CHARLOTTE, CHARLOTTE_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
            realm.getUsers().add(createUser(DAN, DAN_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
        } else {
            realm.getUsers().add(createUser(ALICE, ALICE_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE), ALICE_FIRST_NAME, ALICE_LAST_NAME, ALICE_EMAIL_VERIFIED));
            realm.getUsers().add(createUser(BOB, BOB_PASSWORD, Arrays.asList(USER_ROLE)));
            realm.getUsers().add(createUser(CHARLIE, CHARLIE_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
        }
        return realm;
    }

    private static ClientRepresentation createWebAppClient(String clientId, String clientSecret, String clientHostName, int clientPort,
                                                           String clientApp, boolean directAccessGrantEnabled, String multiTenancyRedirectUri) throws Exception {
        return createWebAppClient(clientId, clientSecret, clientHostName, clientPort, clientApp, directAccessGrantEnabled, null, multiTenancyRedirectUri);
    }

    private static ClientRepresentation createWebAppClient(String clientId, String clientSecret, String clientHostName, int clientPort,
                                                           String clientApp, boolean directAccessGrantEnabled, String allowedOrigin, String multiTenancyRedirectUri) throws Exception {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret(clientSecret);
        //client.setRedirectUris(Arrays.asList("*"));
        if (multiTenancyRedirectUri != null) {
            client.setRedirectUris(Arrays.asList(multiTenancyRedirectUri));
        } else {
            client.setRedirectUris(Arrays.asList("http://" + clientHostName + ":" + clientPort + "/" + clientApp + "/*"));
        }
        client.setEnabled(true);

        if (clientId.equals(MULTIPLE_SCOPE_APP) || clientId.equals(SINGLE_SCOPE_APP)) {
            client.setOptionalClientScopes(new ArrayList<>());
            client.setDefaultClientScopes(new ArrayList<>());
            client.getDefaultClientScopes().add("web-origins");
            client.getDefaultClientScopes().add("acr");
            client.getOptionalClientScopes().add("address");
            client.getOptionalClientScopes().add("email");
            client.getOptionalClientScopes().add("profile");
            client.getOptionalClientScopes().add("phone");
            client.getDefaultClientScopes().add("roles");
            client.getOptionalClientScopes().add("offline_access");
            client.getOptionalClientScopes().add("microprofile-jwt");
        }
        client.setDirectAccessGrantsEnabled(directAccessGrantEnabled);
        if (allowedOrigin != null) {
            client.setWebOrigins(Collections.singletonList(allowedOrigin));
        }
        OIDCAdvancedConfigWrapper oidcAdvancedConfigWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        oidcAdvancedConfigWrapper.setUseJwksUrl(false);
        KEYSTORE_CLASSPATH = Objects.requireNonNull(KeycloakConfiguration.class.getClassLoader().getResource("")).getPath();
        File ksFile = new File(KEYSTORE_CLASSPATH + KEYSTORE_FILE_NAME);
        if (ksFile.exists()) {
            InputStream stream = findFile(KEYSTORE_CLASSPATH + KEYSTORE_FILE_NAME);
            KeyStore keyStore = KeyStore.getInstance(PKCS12_KEYSTORE_TYPE);
            keyStore.load(stream, KEYSTORE_PASS.toCharArray());
            client.getAttributes().put("jwt.credential.certificate", Base64.getEncoder().encodeToString(keyStore.getCertificate(KEYSTORE_ALIAS).getEncoded()));
        } else {
            caGenerationTool = CAGenerationTool.builder()
                    .setBaseDir(KEYSTORE_CLASSPATH)
                    .setRequestIdentities(CAGenerationTool.Identity.values()) // Create all identities.
                    .build();
            X500Principal principal = new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=OcspResponder");
            X509Certificate rsaCert = caGenerationTool.createIdentity(KEYSTORE_ALIAS, principal, RSA_KEYSTORE_FILE_NAME, CAGenerationTool.Identity.CA);
            client.getAttributes().put("jwt.credential.certificate", Base64.getEncoder().encodeToString(rsaCert.getEncoded()));
        }
        return client;
    }

    private static ClientRepresentation createBearerOnlyClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setBearerOnly(true);
        client.setEnabled(true);
        return client;
    }

    private static InputStream findFile(String keystoreFile) {
        try {
            return new FileInputStream(keystoreFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static UserRepresentation createUser(String username, String password, List<String> realmRoles) {
        return createUser(username, password, realmRoles, username, username, false);
    }

    private static UserRepresentation createUser(String username, String password, List<String> realmRoles, String firstName, String lastName, boolean emailVerified) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(emailVerified);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(realmRoles);
        user.setEmail(username + "@gmail.com");

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.getCredentials().add(credential);
        return user;
    }

}