/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.manual.elytron.oidc;

import static org.wildfly.test.manual.elytron.oidc.OidcBaseTest.MULTIPLE_SCOPE_APP;
import static org.wildfly.test.manual.elytron.oidc.OidcBaseTest.SINGLE_SCOPE_APP;

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
 * @author <a href="mailto:prpaul@redhat.com">Prarthona Paul</a>
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
    public static final String ALICE_FIRST_NAME = "Alice";
    public static final String ALICE_LAST_NAME = "Smith";
    public static final boolean ALICE_EMAIL_VERIFIED = true;

    public enum ClientAppType {
        OIDC_SCOPE_CLIENT
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
        return createRealm(realmName, clientSecret, clientHostName, clientPort, clientApps);
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

    private static RealmRepresentation createRealm(String name, String clientSecret,
                                                   String clientHostName, int clientPort, Map<String, ClientAppType> clientApps) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(3);
        realm.setSsoSessionMaxLifespan(3);

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation(USER_ROLE, null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation(JBOSS_ADMIN_ROLE, null, false));

        for (Map.Entry<String, ClientAppType> entry : clientApps.entrySet()) {
            String clientApp = entry.getKey();
            ClientRepresentation clientRepresentation = createWebAppClient(clientApp, clientSecret, clientHostName, clientPort, clientApp, false);
            realm.getClients().add(clientRepresentation);
        }

        realm.getUsers().add(createUser(ALICE, ALICE_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE), ALICE_FIRST_NAME, ALICE_LAST_NAME, ALICE_EMAIL_VERIFIED));
        realm.getUsers().add(createUser(BOB, BOB_PASSWORD, Arrays.asList(USER_ROLE)));
        realm.getUsers().add(createUser(CHARLIE, CHARLIE_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
        return realm;
    }

    private static ClientRepresentation createWebAppClient(String clientId, String clientSecret, String clientHostName, int clientPort,
                                                           String clientApp, boolean directAccessGrantEnabled) {
        return createWebAppClient(clientId, clientSecret, clientHostName, clientPort, clientApp, directAccessGrantEnabled, null);
    }

    private static ClientRepresentation createWebAppClient(String clientId, String clientSecret, String clientHostName, int clientPort,
                                                           String clientApp, boolean directAccessGrantEnabled, String allowedOrigin) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret(clientSecret);
        //client.setRedirectUris(Arrays.asList("*"));
        client.setRedirectUris(Arrays.asList("http://" + clientHostName + ":" + clientPort + "/" + clientApp  + "/*"));
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
        return client;
    }

    private static UserRepresentation createUser(String username, String password, List<String> realmRoles) {
        return createUser(username, password, realmRoles, username, null, false);
    }

    private static UserRepresentation createUser(String username, String password, List<String> realmRoles, String firstName, String lastName, Boolean emailVerified) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(realmRoles);
        user.setEmail(username + "@gmail.com");
        user.setEmailVerified(emailVerified);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.getCredentials().add(credential);
        return user;
    }

}