/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Keycloak configuration for testing.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class KeycloakConfiguration {

    private static final String USER_ROLE = "user";
    public static final String JBOSS_ADMIN_ROLE = "JBossAdmin";
    public static final String ALICE = "alice";
    public static final String ALICE_PASSWORD = "alice123+";
    public static final String BOB = "bob";
    public static final String BOB_PASSWORD = "bob123+";
    public static final String CHARLIE = "charlie";
    public static final String CHARLIE_PASSWORD = "charlie123+";
    public static final String ALLOWED_ORIGIN = "http://somehost";

    // the users below are for multi-tenancy tests specifically
    public static final String TENANT1_USER = "tenant1_user";
    public static final String TENANT1_PASSWORD = "tenant1_password";
    public static final String TENANT2_USER = "tenant2_user";
    public static final String TENANT2_PASSWORD = "tenant2_password";
    public static final String CHARLOTTE = "charlotte";
    public static final String CHARLOTTE_PASSWORD =" charlotte123+";
    public static final String DAN = "dan";
    public static final String DAN_PASSWORD =" dan123+";
    public static final String TENANT1_REALM = "tenant1";
    public static final String TENANT2_REALM = "tenant2";
    public static final String TENANT1_ENDPOINT = "/tenant1";
    public static final String TENANT2_ENDPOINT = "/tenant2";

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
                                                             String clientHostName, int clientPort, Map<String, ClientAppType> clientApps) {
        return createRealm(realmName, clientSecret, clientHostName, clientPort, clientApps, 3, 3, false);
    }

    public static RealmRepresentation getRealmRepresentation(final String realmName, String clientSecret,
                                                             String clientHostName, int clientPort, Map<String, ClientAppType> clientApps,
                                                             int accessTokenLifespan, int ssoSessionMaxLifespan, boolean multiTenancyApp) {
        return createRealm(realmName, clientSecret, clientHostName, clientPort, clientApps, accessTokenLifespan, ssoSessionMaxLifespan, multiTenancyApp);
    }

    public static String getAdminAccessToken(String authServerUrl) {
        RequestSpecification requestSpecification = RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", KeycloakContainer.ADMIN_USER)
                .param("password", KeycloakContainer.ADMIN_PASSWORD)
                .param("client_id", "admin-cli");

        Response response = requestSpecification.when().post(authServerUrl + "/realms/master/protocol/openid-connect/token");

        final long deadline = System.currentTimeMillis() + 180000;
        while (response.getStatusCode() != 200) {
            // the Keycloak admin user isn't available yet, keep trying until it is to ensure we can obtain the token
            // needed to set up the realms for the test
            response = requestSpecification.when().post(authServerUrl + "/realms/master/protocol/openid-connect/token");
            if (System.currentTimeMillis() > deadline) {
                return null;
            }
        }

        return response.as(AccessTokenResponse.class).getToken();
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
                                                   String clientHostName, int clientPort, Map<String, ClientAppType> clientApps) {
        return createRealm(name, clientSecret, clientHostName, clientPort, clientApps, 3, 3, false);
    }

    private static RealmRepresentation createRealm(String name, String clientSecret,
                                                   String clientHostName, int clientPort, Map<String, ClientAppType> clientApps,
                                                   int accessTokenLifespan, int ssoSessionMaxLifespan, boolean multiTenancyApp) {
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
                    multiTenancyRedirectUri = "http://" + clientHostName + ":" + clientPort + "/" + clientApp  + TENANT1_ENDPOINT;
                } else if (name.equals(TENANT2_REALM)) {
                    multiTenancyRedirectUri = "http://" + clientHostName + ":" + clientPort + "/" + clientApp  + TENANT2_ENDPOINT;
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
            realm.getUsers().add(createUser(ALICE, ALICE_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
            realm.getUsers().add(createUser(BOB, BOB_PASSWORD, Arrays.asList(USER_ROLE)));
            realm.getUsers().add(createUser(CHARLIE, CHARLIE_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
        }
        return realm;
    }

    private static ClientRepresentation createWebAppClient(String clientId, String clientSecret, String clientHostName, int clientPort,
                                                           String clientApp, boolean directAccessGrantEnabled, String multiTenancyRedirectUri) {
        return createWebAppClient(clientId, clientSecret, clientHostName, clientPort, clientApp, directAccessGrantEnabled, null, multiTenancyRedirectUri);
    }

    private static ClientRepresentation createWebAppClient(String clientId, String clientSecret, String clientHostName, int clientPort,
                                                           String clientApp, boolean directAccessGrantEnabled, String allowedOrigin, String multiTenancyRedirectUri) {
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
        client.setDirectAccessGrantsEnabled(directAccessGrantEnabled);
        if (allowedOrigin != null) {
            client.setWebOrigins(Collections.singletonList(allowedOrigin));
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

    private static UserRepresentation createUser(String username, String password, List<String> realmRoles) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setFirstName(username);
        user.setLastName(username);
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