/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.participant.jwt;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.function.Supplier;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.jwt.TokenUtil;
import org.wildfly.test.integration.microprofile.lra.participant.jwt.model.JwtBooking;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Smoke test to verify JWT integration with LRA participants in WildFly.
 * Tests that JWT tokens are properly injected via CDI and that authentication
 * is enforced at the container level.
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableLRAAndJwtExtensionsSetupTask.class)
public class LRAServiceTokenTestCase {

    private static final String LRA_COORDINATOR_URL_KEY = "lra.coordinator.url";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer";
    private static final String PRINCIPAL_NAME = "testUser";
    private static final String SUBSCRIBER_GROUP = "Subscriber";
    private static final URL KEY_LOCATION = LRAServiceTokenTestCase.class.getResource("/org/wildfly/test/integration/microprofile/lra/participant/jwt/private.pem");

    @ArquillianResource
    public URL baseURL;

    public CloseableHttpClient client;
    private String jwtToken;

    @Before
    public void before() throws Exception {
        System.setProperty(LRA_COORDINATOR_URL_KEY, "http://localhost:8080/lra-coordinator/lra-coordinator");
        client = HttpClientBuilder.create().build();

        // Generate JWT token for authentication
        Supplier<PrivateKey> keySupplier = TokenUtil.createKeySupplier(
            Paths.get(KEY_LOCATION.toURI()).toAbsolutePath().toString());
        String currentDate = java.time.LocalDate.now().toString();
        jwtToken = TokenUtil.generateJWT(keySupplier, PRINCIPAL_NAME, currentDate, SUBSCRIBER_GROUP);
    }

    @After
    public void after() throws IOException {
        try {
            if (client != null) {
                client.close();
            }
        } finally {
            System.clearProperty(LRA_COORDINATOR_URL_KEY);
        }
    }

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        // The coordinator-side JWT settings (lra.http-client.providers / lra.security.service-token.location)
        // are configured on the server by EnableLRAAndJwtExtensionsSetupTask, not here: the coordinator reads
        // them through its own module classloader and cannot see this WAR's config or classes.
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "lra-service-token-test.war")
            .addPackages(true,
                "org.wildfly.test.integration.microprofile.lra.participant.jwt")
            .addClasses(LRAServiceTokenTestCase.class,
                EnableLRAAndJwtExtensionsSetupTask.class,
                        ManagementServerSetupTask.class,
                TokenUtil.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource(LRAServiceTokenTestCase.class.getResource("/org/wildfly/test/integration/microprofile/lra/participant/jwt/web.xml"), "web.xml")
            .addAsManifestResource(new StringAsset(
                "mp.jwt.verify.publickey.location=META-INF/public.pem\n" +
                "mp.jwt.verify.issuer=quickstart-jwt-issuer\n"),
                "microprofile-config.properties")
            .addAsManifestResource(LRAServiceTokenTestCase.class.getResource("/org/wildfly/test/integration/microprofile/lra/participant/jwt/public.pem"), "public.pem");

        return webArchive;
    }

    /**
     * Smoke test: Verify JWT is properly injected via CDI in LRA participants.
     * This tests the WildFly integration of MicroProfile JWT with LRA.
     */
    @Test
    public void testJwtInjectionInLraParticipant() throws Exception {
        // Make an authenticated request that starts an LRA
        JwtBooking booking = bookHotelWithJwt("Test-Hotel");

        // Verify JWT principal was captured via CDI injection
        Assert.assertNotNull("Booking should be created", booking);
        Assert.assertEquals("JWT principal should be injected via CDI",
            PRINCIPAL_NAME, booking.getBookingPrincipal());
        Assert.assertNotNull("LRA ID should be set", booking.getId());
    }

    /**
     * Smoke test: Verify authentication is enforced at container level.
     * This tests that WildFly's security subsystem properly integrates with JAX-RS endpoints.
     */
    @Test
    public void testAuthenticationIsEnforced() throws Exception {
        HttpPost request = new HttpPost(
            new URIBuilder(uriFrom(baseURL.toURI(), JwtLraParticipant.JWT_LRA_PARTICIPANT_PATH))
                .addParameter("hotelName", "No-Auth-Hotel")
                .build());

        // NO Authorization header

        try (CloseableHttpResponse response = client.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            // Verify request is rejected (401 or 403 depending on container configuration)
            Assert.assertTrue("Unauthenticated request must be rejected",
                status == 401 || status == 403);
        }
    }

    /**
     * Smoke test: Verify end-to-end JWT token propagation in LRA lifecycle.
     * This tests the feature from https://github.com/jbosstm/lra/pull/321 where:
     * 1. Participant sends JWT token when communicating with coordinator (starting LRA)
     * 2. Coordinator sends JWT token when calling participant callbacks (@Complete/@Compensate)
     *
     * This test uses the REAL LRA coordinator running in WildFly to verify bidirectional
     * JWT propagation works end-to-end.
     */
    @Test
    public void testJwtTokenPropagationInLraLifecycle() throws Exception {
        // Step 1: Participant sends JWT token to start LRA
        // The bookRoom() method is annotated with @LRA(REQUIRED) which contacts the
        // real LRA coordinator. The participant's JWT token propagates to the coordinator.
        JwtBooking booking = bookHotelWithJwt("End-to-End-JWT-Test");
        Assert.assertNotNull("Booking should be created", booking);
        Assert.assertEquals("JWT principal should be captured during booking",
            PRINCIPAL_NAME, booking.getBookingPrincipal());
        String lraId = booking.getId();
        String lraUid = extractLraUid(lraId);

        // Step 2: Close the LRA via the coordinator
        // This triggers the REAL coordinator to call back the participant's @Complete method
        // The coordinator should send its service account JWT token in the callback
        closeLraViaCoordinator(lraUid);

        // Step 3: Poll for callback completion (coordinator callback is async)
        // Wait up to 10 seconds for the booking status to change from PROVISIONAL to CONFIRMED
        JwtBooking completedBooking = pollForBookingStatus(lraId, JwtBooking.BookingStatus.CONFIRMED, 10000);
        Assert.assertEquals("Booking should be confirmed after LRA close",
            JwtBooking.BookingStatus.CONFIRMED, completedBooking.getStatus());

        // The coordinator should have sent its JWT token in the callback
        // If JWT propagation works, completePrincipal should contain the coordinator's service account
        Assert.assertNotNull("Complete callback should have been invoked",
            completedBooking.getCompletePrincipal());

        // Note: The actual principal value depends on coordinator configuration
        // This verifies JWT was present and extracted (not null/anonymous)
        Assert.assertFalse("Coordinator should have sent JWT token (not anonymous)",
            "anonymous".equals(completedBooking.getCompletePrincipal()));
    }

    private JwtBooking bookHotelWithJwt(String name) throws Exception {
        HttpPost request = new HttpPost(
            new URIBuilder(uriFrom(baseURL.toURI(), JwtLraParticipant.JWT_LRA_PARTICIPANT_PATH))
                .addParameter("hotelName", name)
                .build());

        request.addHeader(AUTHORIZATION, BEARER + " " + jwtToken);

        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception("Hotel booking problem; response status = " +
                    response.getStatusLine().getStatusCode());
            } else if (response.getEntity() != null) {
                String result = EntityUtils.toString(response.getEntity());
                ObjectMapper obj = new ObjectMapper();
                return obj.readValue(result, JwtBooking.class);
            } else {
                throw new Exception("Hotel booking problem; no entity");
            }
        }
    }

    private String extractLraUid(String lraId) {
        // Extract UID from LRA ID (format: http://host:port/lra-coordinator/lra-coordinator/{uid})
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*/([^/?]+).*");
        java.util.regex.Matcher matcher = pattern.matcher(lraId);
        return matcher.replaceFirst("$1");
    }

    private void closeLraViaCoordinator(String lraUid) throws Exception {
        String coordinatorUrl = System.getProperty(LRA_COORDINATOR_URL_KEY);
        HttpPut request = new HttpPut(coordinatorUrl + "/" + lraUid + "/close");
        request.setHeader("Narayana-LRA-API-version", "1.0");
        request.setEntity(new StringEntity(""));

        try (CloseableHttpResponse response = client.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status != 200 && status != 204) {
                throw new Exception("Failed to close LRA: " + status);
            }
        }
    }

    private JwtBooking getBooking(String lraId) throws Exception {
        // URL encode the LRA ID since it contains slashes and special characters
        String encodedLraId = java.net.URLEncoder.encode(lraId, "UTF-8");
        HttpGet request = new HttpGet(
            uriFrom(baseURL.toURI(),
                JwtLraParticipant.JWT_LRA_PARTICIPANT_PATH + "/" + encodedLraId));

        request.addHeader(AUTHORIZATION, BEARER + " " + jwtToken);

        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception("Failed to get booking: " + response.getStatusLine().getStatusCode());
            }
            String result = EntityUtils.toString(response.getEntity());
            ObjectMapper obj = new ObjectMapper();
            return obj.readValue(result, JwtBooking.class);
        }
    }

    private JwtBooking pollForBookingStatus(String lraId, JwtBooking.BookingStatus expectedStatus, long timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        JwtBooking booking = null;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                booking = getBooking(lraId);
                if (booking.getStatus() == expectedStatus) {
                    return booking;
                }
            } catch (Exception e) {
                // Ignore and retry
            }
            Thread.sleep(500); // Poll every 500ms
        }

        // Return last booking state or throw if never retrieved
        if (booking != null) {
            return booking;
        }
        throw new Exception("Timeout waiting for booking status: " + expectedStatus);
    }

    private static java.net.URI uriFrom(java.net.URI baseURI, String... paths) {
        StringBuilder sb = new StringBuilder(baseURI.toString());
        java.util.Arrays.stream(paths).forEach(s -> sb.append(s.startsWith("/") ? s : "/" + s));
        return java.net.URI.create(sb.toString());
    }
}