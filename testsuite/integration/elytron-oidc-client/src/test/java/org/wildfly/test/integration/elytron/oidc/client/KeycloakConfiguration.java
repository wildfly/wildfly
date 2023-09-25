/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wildfly.test.integration.elytron.oidc.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static org.wildfly.security.http.oidc.Oidc.KEYSTORE_PASS;

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
                                                             String clientHostName, int clientPort, Map<String, ClientAppType> clientApps) throws GeneralSecurityException, IOException, OperatorCreationException {
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
                                                   String clientHostName, int clientPort, Map<String, ClientAppType> clientApps) throws GeneralSecurityException, IOException, OperatorCreationException {
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
            switch (entry.getValue()) {
                case DIRECT_ACCESS_GRANT_OIDC_CLIENT:
                    realm.getClients().add(createWebAppClient(clientApp, clientSecret, clientHostName, clientPort, clientApp, true));
                    break;
                case BEARER_ONLY_CLIENT:
                    realm.getClients().add(createBearerOnlyClient(clientApp));
                    break;
                case CORS_CLIENT:
                    realm.getClients().add(createWebAppClient(clientApp, clientSecret, clientHostName, clientPort, clientApp, true, ALLOWED_ORIGIN));
                    break;
                default:
                    realm.getClients().add(createWebAppClient(clientApp, clientSecret, clientHostName, clientPort, clientApp, false));
            }
        }

        realm.getUsers().add(createUser(ALICE, ALICE_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE), ALICE_FIRST_NAME, ALICE_LAST_NAME, ALICE_EMAIL_VERIFIED));
        realm.getUsers().add(createUser(BOB, BOB_PASSWORD, Arrays.asList(USER_ROLE)));
        realm.getUsers().add(createUser(CHARLIE, CHARLIE_PASSWORD, Arrays.asList(USER_ROLE, JBOSS_ADMIN_ROLE)));
        return realm;
    }

    private static ClientRepresentation createWebAppClient(String clientId, String clientSecret, String clientHostName, int clientPort,
                                                           String clientApp, boolean directAccessGrantEnabled) throws GeneralSecurityException, IOException, OperatorCreationException {
        return createWebAppClient(clientId, clientSecret, clientHostName, clientPort, clientApp, directAccessGrantEnabled, null);
    }

    private static ClientRepresentation createWebAppClient(String clientId, String clientSecret, String clientHostName, int clientPort,
                                                           String clientApp, boolean directAccessGrantEnabled, String allowedOrigin) throws GeneralSecurityException, IOException, OperatorCreationException {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret(clientSecret);
        //client.setRedirectUris(Arrays.asList("*"));
        client.setRedirectUris(Arrays.asList("http://" + clientHostName + ":" + clientPort + "/" + clientApp  + "/*"));
        client.setEnabled(true);
        client.setDirectAccessGrantsEnabled(directAccessGrantEnabled);
        if (allowedOrigin != null) {
            client.setWebOrigins(Collections.singletonList(allowedOrigin));
        }
        OIDCAdvancedConfigWrapper oidcAdvancedConfigWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        oidcAdvancedConfigWrapper.setUseJwksUrl(false);
        OidcBaseTest.KEYSTORE_CLASSPATH = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).getPath();
        String rsaCert = generateKeyStoreFileAndGetCertificate("Rsa", 2048, OidcBaseTest.KEYSTORE_CLASSPATH + OidcBaseTest.KEYSTORE_FILE_NAME, "SHA256WITHRSA");
        client.getAttributes().put("jwt.credential.certificate", rsaCert);
        return client;
    }

    static String generateKeyStoreFileAndGetCertificate(String algorithm, int keySize, String keystorePath, String certSignAlg) throws GeneralSecurityException, IOException, OperatorCreationException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
        keyGen.initialize(keySize);
        KeyPair keys = keyGen.generateKeyPair();
        X509Certificate cert = createCertificate(keys, certSignAlg);
        KeyStore keystore = createKeyStore(cert, keys.getPrivate());
        try (FileOutputStream fileOutputStream = new FileOutputStream(keystorePath)) {
            keystore.store(fileOutputStream, KEYSTORE_PASS.toCharArray());
        }

        KeyStoreConfig keyStoreConfig = new KeyStoreConfig();
        keyStoreConfig.setKeyAlias(OidcBaseTest.KEYSTORE_ALIAS);
        keyStoreConfig.setStorePassword(KEYSTORE_PASS);
        keyStoreConfig.setKeyPassword(KEYSTORE_PASS);
        keyStoreConfig.setRealmCertificate(true);
        return Base64.getEncoder().encodeToString(cert.getEncoded());
    }

    private static KeyStore createKeyStore(X509Certificate certificate, PrivateKey privateKey) throws IOException, GeneralSecurityException {
        KeyStore keyStore = createEmptyKeyStore();
        keyStore.setCertificateEntry("jwtKeystore", certificate);
        keyStore.setKeyEntry("jwtKeystore", privateKey, "password".toCharArray(), new Certificate[]{certificate});
        return keyStore;
    }

    private static KeyStore createEmptyKeyStore() throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null,null);
        return keyStore;
    }

    private static X509Certificate createCertificate(KeyPair keyPair, String certSignAlg) throws GeneralSecurityException, OperatorCreationException {
        X500Name subject = new X500Name("cn=localhost");
        Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);    //1 day before now
        Date endDate = new Date(System.currentTimeMillis() + (long) 2 * 365 * 24 * 60 * 60 * 1000); //2 years from now
        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(subject, BigInteger.valueOf(System.currentTimeMillis()), startDate, endDate, subject, keyPair.getPublic());
        final ContentSigner contentSigner = new JcaContentSignerBuilder(certSignAlg).build(keyPair.getPrivate());

        return new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider())
                .getCertificate(certGen.build(contentSigner));
    }

    private static ClientRepresentation createBearerOnlyClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setBearerOnly(true);
        client.setEnabled(true);
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