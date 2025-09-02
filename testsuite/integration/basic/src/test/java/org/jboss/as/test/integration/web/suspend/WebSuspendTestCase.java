/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.suspend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.Permission;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.management.MBeanServerPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.security.ControllerPermission;
import org.jboss.as.server.suspend.SuspensionStateProvider;
import org.jboss.as.test.integration.web.suspend.servlet.SuspendStateServlet;
import org.jboss.as.test.integration.web.suspend.servlet.SuspendStateServletRequestListener;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that an accepted request properly blocks server suspension for the complete duration of a request,
 * i.e. from {@link jakarta.servlet.ServletRequestListener#requestInitialized(jakarta.servlet.ServletRequestEvent)} to {@link jakarta.servlet.ServletRequestListener#requestDestroyed(jakarta.servlet.ServletRequestEvent)},
 * and that requests fail with the expected status code once suspended.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebSuspendTestCase {

    @Deployment
    public static WebArchive deployment() {
        Permission[] permissions = new Permission[] {
                new MBeanServerPermission("createMBeanServer"),
                ControllerPermission.CAN_ACCESS_MODEL_CONTROLLER,
                ControllerPermission.CAN_ACCESS_IMMUTABLE_MANAGEMENT_RESOURCE_REGISTRATION,
        };
        return ShrinkWrap.create(WebArchive.class, WebSuspendTestCase.class.getSimpleName() + ".war")
                .addPackage(SuspendStateServlet.class.getPackage())
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(permissions), "permissions.xml")
                .setManifest(new StringAsset("""
Manifest-Version: 1.0
Dependencies: org.jboss.as.controller
"""))
                ;
    }

    private static final String RUNNING = SuspensionStateProvider.State.RUNNING.toString();
    private static final String SUSPENDING = SuspensionStateProvider.State.SUSPENDING.toString();
    private static final Duration REQUEST_DURATION = Duration.ofMillis(TimeoutUtil.adjust(1000));
    private static final Set<String> EVENTS = Set.of(SuspendStateServletRequestListener.INIT_EVENT_NAME, SuspendStateServlet.EVENT_NAME, SuspendStateServletRequestListener.DESTROY_EVENT_NAME);

    @ArquillianResource(SuspendStateServlet.class)
    private URL baseURL;
    @ArquillianResource
    private ManagementClient client;

    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    @After
    public void close() {
        this.executor.shutdown();
    }

    @Test
    public void init() throws Exception {
        this.test(SuspendStateServletRequestListener.INIT_EVENT_NAME, SUSPENDING, SUSPENDING, SUSPENDING);
    }

    @Test
    public void service() throws Exception {
        this.test(SuspendStateServlet.EVENT_NAME, RUNNING, SUSPENDING, SUSPENDING);
    }

    @Test
    public void destroy() throws Exception {
        this.test(SuspendStateServletRequestListener.DESTROY_EVENT_NAME, RUNNING, RUNNING, SUSPENDING);
    }

    public void test(String longEvent, String expectedInitStateWhileSuspending, String expectedServiceStateWhileSuspending, String expectedDestroyStateWhileSuspending) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).executor(this.executor).build();

        Consumer<HttpResponse<Void>> verifyOK = new StatusCodeVerification(HttpURLConnection.HTTP_OK);
        Consumer<HttpResponse<Void>> verifyUnavailable = new StatusCodeVerification(HttpURLConnection.HTTP_UNAVAILABLE);

        URI uri = SuspendStateServlet.createURI(this.baseURL, longEvent, REQUEST_DURATION);

        CompletableFuture<HttpResponse<Void>> response = request(client, uri);

        // N.B. The suspend state headers for the request destroyed event will always reflect values recorded by the previous request, since this event fires after the response was committed.
        verifyOK.accept(response.join()); // Don't verify state of initial request

        response = request(client, uri);

        verifyOK.andThen(new SuspendedStateVerification(Map.of(
                SuspendStateServletRequestListener.DESTROY_EVENT_NAME, RUNNING,
                SuspendStateServletRequestListener.INIT_EVENT_NAME, RUNNING,
                SuspendStateServlet.EVENT_NAME, RUNNING)))
                .accept(response.join());

        response = request(client, uri);

        // Initiate suspend part-way through request duration
        Thread.sleep(REQUEST_DURATION.toMillis() / 2);

        this.suspend();

        try {
            // Request was accepted, request events should never see SUSPENDED state
            verifyOK.andThen(new SuspendedStateVerification(Map.of(
                    SuspendStateServletRequestListener.DESTROY_EVENT_NAME, RUNNING,
                    SuspendStateServletRequestListener.INIT_EVENT_NAME, expectedInitStateWhileSuspending,
                    SuspendStateServlet.EVENT_NAME, expectedServiceStateWhileSuspending)))
                    .accept(response.join());

            response = request(client, uri);

            // We were suspended, no suspend state recorded
            verifyUnavailable.andThen(new SuspendedStateVerification(Map.of())).accept(response.join());
        } finally {
            this.resume();

            response = request(client, uri);

            // State of destroy event reflects state recorded during last successful request
            verifyOK.andThen(new SuspendedStateVerification(Map.of(
                    SuspendStateServletRequestListener.DESTROY_EVENT_NAME, expectedDestroyStateWhileSuspending,
                    SuspendStateServletRequestListener.INIT_EVENT_NAME, RUNNING,
                    SuspendStateServlet.EVENT_NAME, RUNNING)))
                    .accept(response.join());

            // One more to verify state during request destroy event
            response = request(client, uri);

            verifyOK.andThen(new SuspendedStateVerification(Map.of(
                    SuspendStateServletRequestListener.DESTROY_EVENT_NAME, RUNNING,
                    SuspendStateServletRequestListener.INIT_EVENT_NAME, RUNNING,
                    SuspendStateServlet.EVENT_NAME, RUNNING)))
                    .accept(response.join());
        }
    }

    private static CompletableFuture<HttpResponse<Void>> request(HttpClient client, URI uri) {
        return client.sendAsync(HttpRequest.newBuilder(uri).method("GET", BodyPublishers.noBody()).build(), BodyHandlers.discarding());
    }

    private void suspend() throws IOException {
        execute(ModelDescriptionConstants.SUSPEND);
    }

    private void resume() throws IOException {
        execute(ModelDescriptionConstants.RESUME);
    }

    private void execute(String operation) throws IOException {
        ModelNode result = this.client.getControllerClient().execute(Util.createOperation(operation, PathAddress.EMPTY_ADDRESS));
        assertThat(result.get(ModelDescriptionConstants.OUTCOME).asString()).as(result.toString()).isEqualTo(ModelDescriptionConstants.SUCCESS);
    }

    private class StatusCodeVerification implements Consumer<HttpResponse<Void>> {
        private final int expectedStatusCode;

        StatusCodeVerification(int expectedStatusCode) {
            this.expectedStatusCode = expectedStatusCode;
        }

        @Override
        public void accept(HttpResponse<Void> response) {
            assertThat(response.statusCode()).isEqualTo(this.expectedStatusCode);
        }
    }

    private class SuspendedStateVerification implements Consumer<HttpResponse<Void>> {
        private final Map<String, String> expectedStates;

        SuspendedStateVerification(Map<String, String> expectedStates) {
            this.expectedStates = expectedStates;
        }

        @Override
        public void accept(HttpResponse<Void> response) {
            for (String event : EVENTS) {
                String state = this.expectedStates.get(event);
                if (state != null) {
                    assertThat(response.headers().allValues(event)).as(event).containsExactly(state);
                } else {
                    assertThat(response.headers().allValues(event)).as(event).isEmpty();
                }
            }
        }
    }
}
