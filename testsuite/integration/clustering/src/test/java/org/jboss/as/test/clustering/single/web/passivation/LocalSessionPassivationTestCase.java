/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.web.passivation;

import static org.assertj.core.api.Assertions.*;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.servlet.http.HttpServletResponse;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.PassivationEventCollector;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

/**
 * Validates the correctness of session activation/passivation events for a distributed session manager using a local,
 * passivating cache with time-based (idle-threshold) eviction.
 *
 * @author Radoslav Husar
 */
public abstract class LocalSessionPassivationTestCase {
    static final Duration COMMIT_GRACE_PERIOD = TimeoutUtil.adjust(Duration.ofSeconds(4L));

    static WebArchive getBaseDeployment(String moduleName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, moduleName + ".war");
        war.addClasses(SessionOperationServlet.class, PassivationEventCollector.class);
        war.setWebXML(SimpleServlet.class.getPackage(), "web.xml");
        return war;
    }

    private final Duration idleDuration;

    LocalSessionPassivationTestCase(Duration idleDuration) {
        this.idleDuration = idleDuration;
    }

    @Test
    public void test(@ArquillianResource(SessionOperationServlet.class) URL baseURL) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).executor(executor).build();
            HttpClient otherClient = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).executor(executor).build();

            // Create a session and set an attribute
            HttpResponse<Void> putResponse = client.send(HttpRequest.newBuilder(SessionOperationServlet.createURI(baseURL, "foo", "bar")).PUT(BodyPublishers.noBody()).build(), BodyHandlers.discarding());
            assertThat(putResponse.statusCode()).isEqualTo(HttpServletResponse.SC_OK);
            String sessionId = assertThat(putResponse.headers().firstValue(SessionOperationServlet.SESSION_ID)).isPresent().actual().get();
            assertThat(putResponse.headers().allValues(SessionOperationServlet.PASSIVATIONS)).isEmpty();
            assertThat(putResponse.headers().allValues(SessionOperationServlet.ACTIVATIONS)).isEmpty();

            for (int i = 1; i <= 5; ++i) {
                if (!this.idleDuration.isNegative() && !this.idleDuration.isZero()) {
                    // Wait until idle, although idleness does not start until write-back completes
                    Thread.sleep(this.idleDuration.toMillis());
                }

                // Create another session, to initiate eviction/passivation
                assertThat(otherClient.send(HttpRequest.newBuilder(SessionOperationServlet.createURI(baseURL, "baz", "qux")).PUT(BodyPublishers.noBody()).build(), BodyHandlers.discarding()).statusCode()).isEqualTo(HttpServletResponse.SC_OK);
                // Use other client to look at passivation events for original session
                Callable<Integer> events = () -> otherClient.send(HttpRequest.newBuilder(SessionOperationServlet.createURI(baseURL, Map.of(SessionOperationServlet.SESSION_ID, List.of(sessionId)))).method("HEAD", BodyPublishers.noBody()).build(), BodyHandlers.discarding()).headers().allValues(SessionOperationServlet.PASSIVATIONS).size();
                Awaitility.await("Await receipt of passivation event")
                        .atMost(COMMIT_GRACE_PERIOD)
                        .pollInSameThread()
                        .pollDelay(Duration.ZERO)
                        .pollInterval(Duration.ofMillis(100L))
                        .until(events, Matchers.greaterThanOrEqualTo(i));
                // Delete other session before it becomes idle
                assertThat(otherClient.send(HttpRequest.newBuilder(SessionOperationServlet.createURI(baseURL)).DELETE().build(), BodyHandlers.discarding()).statusCode()).isEqualTo(HttpServletResponse.SC_OK);

                Instant preInvoke = Instant.now().truncatedTo(ChronoUnit.MILLIS);

                HttpResponse<Void> response = client.send(HttpRequest.newBuilder(SessionOperationServlet.createURI(baseURL, "foo")).GET().build(), BodyHandlers.discarding());
                assertThat(response.statusCode()).isEqualTo(HttpServletResponse.SC_OK);
                assertThat(response.headers().firstValue(SessionOperationServlet.SESSION_ID)).hasValue(sessionId);
                assertThat(response.headers().firstValue(SessionOperationServlet.RESULT)).hasValue("bar");
                List<Instant> passivations = response.headers().allValues(SessionOperationServlet.PASSIVATIONS).stream().mapToLong(Long::parseLong).mapToObj(Instant::ofEpochMilli).toList();
                List<Instant> activations = response.headers().allValues(SessionOperationServlet.ACTIVATIONS).stream().mapToLong(Long::parseLong).mapToObj(Instant::ofEpochMilli).toList();

                assertThat(passivations).hasSize(i);
                assertThat(activations).hasSize(i);

                Instant lastPassivation = passivations.get(passivations.size() - 1);
                Instant lastActivation = activations.get(activations.size() - 1);
                if (activations.size() > 1) {
                    assertThat(lastPassivation).isAfter(activations.get(activations.size() - 2).plus(this.idleDuration));
                }
                assertThat(lastPassivation).isBefore(lastActivation);
                assertThat(lastActivation).isAfterOrEqualTo(preInvoke);
            }

            assertThat(client.send(HttpRequest.newBuilder(SessionOperationServlet.createURI(baseURL)).DELETE().build(), BodyHandlers.discarding()).statusCode()).isEqualTo(HttpServletResponse.SC_OK);
        } finally {
            executor.shutdown();
        }
    }
}
