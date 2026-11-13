/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.byteman.agent.submit.Submit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.Server;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildFlyRunner;

import static org.awaitility.Awaitility.await;

/**
 * WFLY-19112
 */
@RunWith(WildFlyRunner.class)
@RunAsClient
@ServerControl(manual = true)
public class HealthShutdownRaceTestCase {

    private static final String SERVER_CONFIG_NAME = "standalone.xml";
    private static final String SUSPEND_MARKER = "[BYTEMAN] shutdown waiting for a health request";
    private static final Duration MARKER_TIMEOUT = TimeoutUtil.adjust(Duration.ofMillis(60000));
    private static final Duration POLL_WAIT_TIME = TimeoutUtil.adjust(Duration.ofMillis(100));

    private static final String BYTEMAN_ADDRESS = System.getProperty("byteman.server.ipaddress",
            Submit.DEFAULT_ADDRESS);
    private static final Integer BYTEMAN_PORT = Integer.getInteger("byteman.server.port", Submit.DEFAULT_PORT);
    private static final Boolean BYTEMAN_POLICY = Boolean.getBoolean("byteman.policy");
    private static final Submit BYTEMAN = new Submit(BYTEMAN_ADDRESS, BYTEMAN_PORT);

    private static final String JVM_ARGS = "jvm.args";
    private static final String ORIGINAL_JVM_ARGS = System.getProperty(JVM_ARGS, "");
    private static final String BASE_DIR = System.getProperty("basedir", "./");

    @Inject
    protected static ServerController container;

    @Before
    public void startContainer() {
        System.setProperty(JVM_ARGS, ORIGINAL_JVM_ARGS
                + " -Djboss.modules.system.pkgs=org.jboss.byteman"
                + " -javaagent:" + BASE_DIR + "/target/lib/byteman.jar=listener:true,port:" + BYTEMAN_PORT
                + ",address:" + BYTEMAN_ADDRESS + ",policy:" + BYTEMAN_POLICY);
        container.start(SERVER_CONFIG_NAME, Server.StartMode.NORMAL);
    }

    @After
    public void stopContainer() {
        System.setProperty(JVM_ARGS, ORIGINAL_JVM_ARGS);
        if (container.isStarted()) {
            container.stop();
        }
    }

    @Test
    public void testHealthReadyEndpointWhenShutdownStartsDuringRequest() throws Exception {
        final Path serverLog = Paths.get(System.getProperty("jboss.home"), "standalone", "log", "server.log");
        final String healthURL = "http://" + TestSuiteEnvironment.getServerAddress() + ":"
                + TestSuiteEnvironment.getServerPort() + "/health/ready";

        BYTEMAN.addRulesFromResources(Collections.singletonList(
                HealthShutdownRaceTestCase.class.getClassLoader()
                        .getResourceAsStream("byteman/" + HealthShutdownRaceTestCase.class.getSimpleName() + ".btm")));

        int statusCode;
        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient()) {
            client.executeAsync(Operations.createOperation("shutdown"), null);
            awaitLogContains(serverLog, SUSPEND_MARKER); // blocked by byteman

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                    CloseableHttpResponse response = httpClient.execute(new HttpGet(healthURL))) {
                statusCode = response.getStatusLine().getStatusCode();
            }
        }
        container.stop();
        final String unwantedExceptionName = ConcurrentModificationException.class.getSimpleName();
        assertFalse(
                "/health/ready logged a " + unwantedExceptionName + " while the server was shutting down:\n",
                logContains(serverLog, line -> line.contains(unwantedExceptionName)));
        assertEquals("a server that has started shutting down must report itself as not ready", 503, statusCode);
    }

    private static void awaitLogContains(Path serverLog, String expected) {
        await()
                .atMost(MARKER_TIMEOUT)
                .pollInterval(POLL_WAIT_TIME)
                .until(() -> logContains(serverLog, line -> line.contains(expected)));
    }

    private static boolean logContains(Path logFile, Predicate<String> predicate) throws IOException {
        try (Stream<String> lines = Files.lines(logFile)) {
            return lines.anyMatch(predicate);
        }
    }
}
