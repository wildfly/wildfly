/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.realm;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.Assert.assertTrue;
import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.security.auth.x500.X500Principal;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.PermissionRef;
import org.wildfly.test.security.common.elytron.SimpleClientSslContext;
import org.wildfly.test.security.common.elytron.SimpleHttpAuthenticationFactory;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimplePermissionMapper;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;
import org.wildfly.test.security.common.elytron.TokenRealm;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Tests that the Preview {@code read-timeout} attribute of the Elytron {@code token-realm} {@code oauth2-introspection} and
 * {@code jwt} validators reaches the validator at runtime.
 * <p>
 * Each test points a validator at a local mock server that is deliberately slow to respond. The {@code read-timeout} is set
 * shorter than that delay, so the validator gives up before the response arrives and the token must not be accepted. The
 * mock servers do return a perfectly valid response once the delay has elapsed, so if the timeout was not honoured the
 * token would be accepted. For the JWT validator the delay is also shorter than the 2000 ms Elytron default, so the default
 * alone would not make the token fail.
 * <p>
 * The {@code connection-timeout} attribute is not covered here, a connection timeout cannot be reliably simulated. It is
 * covered by the WildFly Core subsystem parsing and marshalling tests.
 * <p>
 * To keep the run time of the test suite low the timeouts and delays are very short and only the failure path is exercised.
 * <p>
 * A timed out JWKS request results in a 401 and a timed out introspection request in a 500 (the realm is reported as
 * unavailable). With a timeout longer than the delay both tokens are accepted with a 200.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({
        // The timeout attributes are Preview, the stability level must be set before the realms are added.
        StabilityServerSetupSnapshotRestoreTasks.Preview.class,
        TokenRealmTimeoutTestCase.MockServersSetupTask.class,
        TokenRealmTimeoutTestCase.AllowedJkuSetupTask.class,
        TokenRealmTimeoutTestCase.ElytronSetupTask.class
})
public class TokenRealmTimeoutTestCase {

    /** The read timeout under test, in milliseconds. */
    private static final int READ_TIMEOUT = 500;
    /** How long the mock servers wait before responding. Longer than {@link #READ_TIMEOUT}, shorter than Elytron's 2000 ms JWT default. */
    private static final int RESPONSE_DELAY = 1500;

    private static final String DEPLOYMENT_INTROSPECTION = "TokenRealmIntrospectionTimeout";
    private static final String DEPLOYMENT_JWT = "TokenRealmJwtTimeout";
    private static final String INTROSPECTION_REALM = "token_realm_introspection_timeout";
    private static final String JWT_REALM = "token_realm_jwt_timeout";

    private static final String INDEX_PAGE_CONTENT = "index page content";
    private static final String PRINCIPAL = "elytron@wildfly.org";
    private static final String ISSUER = "issuer1.wildfly.org";
    private static final String KEY_ID = "timeout-test-key";

    private static final String WORK_DIRECTORY_NAME = TokenRealmTimeoutTestCase.class.getSimpleName();
    private static final String STORE_PASSWORD = "changeit";
    private static final Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    @Deployment(name = DEPLOYMENT_INTROSPECTION)
    public static WebArchive introspectionDeployment() {
        return deployment(DEPLOYMENT_INTROSPECTION);
    }

    @Deployment(name = DEPLOYMENT_JWT)
    public static WebArchive jwtDeployment() {
        return deployment(DEPLOYMENT_JWT);
    }

    private static WebArchive deployment(String name) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");
        war.add(new StringAsset(INDEX_PAGE_CONTENT), "index.html");
        war.addAsWebInfResource(TokenRealmTimeoutTestCase.class.getPackage(), "token-realm-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(name), "jboss-web.xml");
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_INTROSPECTION)
    public void testOAuth2IntrospectionReadTimeout(@ArquillianResource URL webAppUrl) throws Exception {
        Utils.makeCallWithTokenAuthn(webAppUrl, "an-opaque-access-token", SC_INTERNAL_SERVER_ERROR);

        assertTrue("The introspection endpoint was never contacted.", MockServers.introspectionRequests.get() > 0);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_JWT)
    public void testJwtJkuReadTimeout(@ArquillianResource URL webAppUrl) throws Exception {
        Utils.makeCallWithTokenAuthn(webAppUrl, MockServers.createSignedJwt(), SC_UNAUTHORIZED);

        assertTrue("The JWKS endpoint was never contacted.", MockServers.jwksRequests.get() > 0);
    }

    /**
     * Starts the slow mock servers and generates the key material needed for the TLS one.
     */
    static class MockServersSetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            // Use the address the server under test uses, which is an IPv4 or IPv6 address depending on how CI runs the tests.
            MockServers.start(CoreUtils.stripSquareBrackets(managementClient.getMgmtAddress()));
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            MockServers.stop();
        }
    }

    /**
     * The JWT validator only fetches keys from a {@code jku} URL that is listed in this system property.
     */
    static class AllowedJkuSetupTask extends AbstractSystemPropertiesServerSetupTask {

        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[] {
                    new DefaultSystemProperty("wildfly.elytron.jwt.allowed.jku.values." + JWT_REALM, MockServers.jwksUrl())
            };
        }
    }

    static class ElytronSetupTask extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            // Trust the certificate of the TLS mock server.
            elements.add(SimpleKeyStore.builder().withName(WORK_DIRECTORY_NAME + "-trust-store")
                    .withPath(org.wildfly.test.security.common.elytron.Path.builder()
                            .withPath(asAbsolutePath(MockServers.trustStoreFile())).build())
                    .withCredentialReference(CredentialReference.builder().withClearText(STORE_PASSWORD).build())
                    .withType("PKCS12")
                    .build());
            elements.add(SimpleTrustManager.builder().withName(WORK_DIRECTORY_NAME + "-trust-manager")
                    .withKeyStore(WORK_DIRECTORY_NAME + "-trust-store")
                    .build());
            elements.add(SimpleClientSslContext.builder().withName(WORK_DIRECTORY_NAME + "-ssl-context")
                    .withTrustManager(WORK_DIRECTORY_NAME + "-trust-manager")
                    .build());

            elements.add(TokenRealm.builder(INTROSPECTION_REALM)
                    .withOauth2Introspection(TokenRealm.oauth2IntrospectionBuilder()
                            .withClientId("timeout-client")
                            .withClientSecret("timeout-secret")
                            .withIntrospectionUrl(MockServers.introspectionUrl())
                            .withReadTimeout(READ_TIMEOUT)
                            .build())
                    .withPrincipalClaim("sub")
                    .build());
            addSecurityElements(elements, INTROSPECTION_REALM, DEPLOYMENT_INTROSPECTION);

            elements.add(TokenRealm.builder(JWT_REALM)
                    .withJwt(TokenRealm.jwtBuilder()
                            .withIssuer(ISSUER)
                            .withClientSslContext(WORK_DIRECTORY_NAME + "-ssl-context")
                            // The mock server certificate is only issued for CN=localhost, the host name is not under test.
                            .withHostNameVerificationPolicy("ANY")
                            .withReadTimeout(READ_TIMEOUT)
                            .build())
                    .withPrincipalClaim("sub")
                    .build());
            addSecurityElements(elements, JWT_REALM, DEPLOYMENT_JWT);

            return elements.toArray(new ConfigurableElement[0]);
        }

        /**
         * Adds the permission mapper, security domain, HTTP authentication factory and application security domain that
         * protect the deployment using the token realm.
         */
        private static void addSecurityElements(List<ConfigurableElement> elements, String realm, String deployment) {
            elements.add(SimplePermissionMapper.builder()
                    .withName(realm + "_permission_mapper")
                    .permissionMapping(SimplePermissionMapper.PermissionMapping.builder()
                            .withPrincipals("\"" + PRINCIPAL + "\"")
                            .withPermissions(PermissionRef.builder()
                                    .className(LoginPermission.class.getName())
                                    .build())
                            .build())
                    .build());
            elements.add(SimpleSecurityDomain.builder()
                    .withName(realm + "_domain")
                    .withDefaultRealm(realm)
                    .withPermissionMapper(realm + "_permission_mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(realm)
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            elements.add(SimpleHttpAuthenticationFactory.builder()
                    .withName(deployment)
                    .withHttpServerMechanismFactory("global")
                    .withSecurityDomain(realm + "_domain")
                    .addMechanismConfiguration(MechanismConfiguration.builder()
                            .withMechanismName("BEARER_TOKEN")
                            .build())
                    .build());
            elements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(deployment)
                    .httpAuthenticationFactory(deployment)
                    .build());
        }
    }

    /**
     * Local mock servers that are slow to respond. Both bind to the address of the server under test on an ephemeral port.
     * <ul>
     * <li>an HTTP OAuth2 token introspection endpoint that reports every token as active</li>
     * <li>an HTTPS endpoint serving the JWKS that holds the key used to sign the test JWT</li>
     * </ul>
     */
    static final class MockServers {

        static final AtomicInteger introspectionRequests = new AtomicInteger();
        static final AtomicInteger jwksRequests = new AtomicInteger();

        private static String host;
        private static File workDirectory;
        private static ExecutorService executor;
        private static HttpServer introspectionServer;
        private static HttpsServer jwksServer;
        private static KeyPair jwtKeys;

        private MockServers() {
        }

        /**
         * @param host the address to bind to and to use in URLs, an IPv6 address must not be enclosed in square brackets
         */
        static synchronized void start(String host) throws Exception {
            MockServers.host = host;
            introspectionRequests.set(0);
            jwksRequests.set(0);

            workDirectory = new File("target", WORK_DIRECTORY_NAME);
            workDirectory.mkdirs();

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            jwtKeys = generator.generateKeyPair();

            // Daemon threads so that a handler still sleeping can never keep the JVM alive.
            executor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, WORK_DIRECTORY_NAME + "-mock-server");
                thread.setDaemon(true);
                return thread;
            });
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(host), 0);

            String introspectionResponse = "{\"active\":true,\"sub\":\"" + PRINCIPAL + "\",\"groups\":[\"userA\"]}";
            introspectionServer = HttpServer.create(address, 0);
            introspectionServer.createContext("/introspect", slowJsonHandler(introspectionRequests, introspectionResponse));
            introspectionServer.setExecutor(executor);
            introspectionServer.start();

            SelfSignedX509CertificateAndSigningKey tlsIdentity = SelfSignedX509CertificateAndSigningKey.builder()
                    .setDn(new X500Principal("CN=localhost"))
                    .setKeyAlgorithmName("RSA")
                    .setKeySize(2048)
                    .setSignatureAlgorithmName("SHA256withRSA")
                    .build();
            X509Certificate certificate = tlsIdentity.getSelfSignedCertificate();

            // What the WildFly server trusts.
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("jwks-server", certificate);
            try (var out = Files.newOutputStream(trustStoreFile().toPath())) {
                trustStore.store(out, STORE_PASSWORD.toCharArray());
            }

            // What the mock server presents.
            KeyStore identity = KeyStore.getInstance("PKCS12");
            identity.load(null, null);
            identity.setKeyEntry("jwks-server", tlsIdentity.getSigningKey(), STORE_PASSWORD.toCharArray(),
                    new X509Certificate[] { certificate });
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(identity, STORE_PASSWORD.toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            jwksServer = HttpsServer.create(address, 0);
            jwksServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            jwksServer.createContext("/jwks", slowJsonHandler(jwksRequests, jwks()));
            jwksServer.setExecutor(executor);
            jwksServer.start();
        }

        static synchronized void stop() throws IOException {
            if (introspectionServer != null) {
                introspectionServer.stop(0);
                introspectionServer = null;
            }
            if (jwksServer != null) {
                jwksServer.stop(0);
                jwksServer = null;
            }
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
            if (workDirectory != null) {
                File[] files = workDirectory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        Files.deleteIfExists(file.toPath());
                    }
                }
                Files.deleteIfExists(workDirectory.toPath());
                workDirectory = null;
            }
        }

        static File trustStoreFile() {
            return new File(workDirectory, "trust.p12");
        }

        static String introspectionUrl() {
            return "http://" + NetworkUtils.formatPossibleIpv6Address(host) + ":" + introspectionServer.getAddress().getPort()
                    + "/introspect";
        }

        static String jwksUrl() {
            return "https://" + NetworkUtils.formatPossibleIpv6Address(host) + ":" + jwksServer.getAddress().getPort() + "/jwks";
        }

        /**
         * Creates a JWT signed by the key published by the JWKS endpoint, the header refers to that endpoint using
         * {@code jku}.
         */
        static String createSignedJwt() throws Exception {
            String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"" + KEY_ID + "\",\"jku\":\"" + jwksUrl() + "\"}";
            String claims = "{\"iss\":\"" + ISSUER + "\",\"sub\":\"" + PRINCIPAL + "\",\"exp\":2051222399,\"groups\":[\"userA\"]}";
            String signingInput = B64_ENCODER.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
                    + B64_ENCODER.encodeToString(claims.getBytes(StandardCharsets.UTF_8));

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(jwtKeys.getPrivate());
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + B64_ENCODER.encodeToString(signature.sign());
        }

        private static String jwks() {
            RSAPublicKey publicKey = (RSAPublicKey) jwtKeys.getPublic();
            return "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"" + KEY_ID + "\",\"n\":\""
                    + B64_ENCODER.encodeToString(unsigned(publicKey.getModulus())) + "\",\"e\":\""
                    + B64_ENCODER.encodeToString(unsigned(publicKey.getPublicExponent())) + "\"}]}";
        }

        private static byte[] unsigned(BigInteger value) {
            byte[] bytes = value.toByteArray();
            if (bytes.length > 1 && bytes[0] == 0) {
                byte[] stripped = new byte[bytes.length - 1];
                System.arraycopy(bytes, 1, stripped, 0, stripped.length);
                return stripped;
            }
            return bytes;
        }

        /**
         * A handler that counts the request, waits for {@link #RESPONSE_DELAY} and then responds with the given JSON.
         */
        private static HttpHandler slowJsonHandler(AtomicInteger requestCounter, String json) {
            return exchange -> {
                requestCounter.incrementAndGet();
                try {
                    exchange.getRequestBody().readAllBytes();
                    Thread.sleep(RESPONSE_DELAY);
                    byte[] body = json.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                } catch (IOException e) {
                    // Expected, by now the client has given up and closed the connection.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
            };
        }
    }
}
