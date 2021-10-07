/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.manual.microprofile.health;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that readiness handling with mp.health.disable-default-procedures=true respects the
 * value of mp.health.default.readiness.empty.response
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class MicroProfileHealthDefaultEmptyReadinessTestBase {

    abstract void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName) throws IOException, InvalidHealthResponseException;

    public static final String MICRO_PROFILE_HEALTH_APPLICATION_WITHOUT_READINESS_TEST_BASE = "MicroProfileHealthApplicationWithoutReadinessTestBase";
    public static final String MICRO_PROFILE_HEALTH_APPLICATION_WITH_SUCCESSFUL_READINESS_TEST_BASE = "MicroProfileHealthApplicationWithSuccessfulReadinessTestBase";

    private static final String CONTAINER_NAME = "microprofile";
    private static final String DISABLE_DEFAULT_PROCEDURES_PROPERTY = "-Dmp.health.disable-default-procedures=true";
    private static final String DEFAULT_READINESS_EMPTY_RESPONSE_PROPERTY = "-Dmp.health.default.readiness.empty.response";
    private static final String MICROPROFILE_SERVER_JVM_ARGS = "microprofile.server.jvm.args";
    private static final String JAVA_VM_ARGUMENTS = "javaVmArguments";

    // deployment does not define any readiness probe
    @Deployment(name = MICRO_PROFILE_HEALTH_APPLICATION_WITHOUT_READINESS_TEST_BASE, managed = false, testable = false)
    @TargetsContainer(CONTAINER_NAME)
    public static Archive<?> deployEmpty() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MICRO_PROFILE_HEALTH_APPLICATION_WITHOUT_READINESS_TEST_BASE + ".war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    // deployment defines one successful readiness probe
    @Deployment(name = MICRO_PROFILE_HEALTH_APPLICATION_WITH_SUCCESSFUL_READINESS_TEST_BASE, managed = false, testable = false)
    @TargetsContainer(CONTAINER_NAME)
    public static Archive<?> deploySuccessful() {
            WebArchive war = ShrinkWrap.create(WebArchive.class, MICRO_PROFILE_HEALTH_APPLICATION_WITH_SUCCESSFUL_READINESS_TEST_BASE + ".war")
            .addClasses(SuccessfulReadinessCheck.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @ContainerResource(CONTAINER_NAME)
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private ContainerController containerController;

    @Before
    public void check() {
        //Assume we are using the full distribution which contains standalone-microprofile.xml
        AssumeTestGroupUtil.assumeFullDistribution();
    }

    @Test
    @InSequence(1)
    public void testApplicationReadinessWithoutDeploymentWithDefaultEmptyReadiness() throws Exception {

        final CompletableFuture<Void> testResultFuture = CompletableFuture.runAsync(() -> {
            boolean connectionEstablished = false;

            do {
                try {
                    checkGlobalOutcome(managementClient, "check-ready", false, null);
                    connectionEstablished = true;
                } catch (ConnectException ce) {
                    // OK, server is not started yet
                } catch (IOException | InvalidHealthResponseException ex) {
                    throw new RuntimeException(ex);
                }

            } while (!connectionEstablished);

            try {
                checkGlobalOutcome(managementClient, "check-ready", false, null);
            } catch (IOException | InvalidHealthResponseException ex) {
                throw new RuntimeException(ex);
            }
        });

        Map<String, String> map = getVMArgMap(DISABLE_DEFAULT_PROCEDURES_PROPERTY);
        containerController.start(CONTAINER_NAME, map);

        try {
            testResultFuture.get(1, TimeUnit.MINUTES);
        } finally {
            containerController.stop(CONTAINER_NAME);
        }
    }

    @Test
    @InSequence(2)
    public void testApplicationReadinessWithEmptyDeploymentWithDefaultEmptyReadiness() throws Exception {

        // deploy the application and stop the container
        containerController.start(CONTAINER_NAME);
        deployer.deploy(MICRO_PROFILE_HEALTH_APPLICATION_WITHOUT_READINESS_TEST_BASE);
        containerController.stop(CONTAINER_NAME);

        // check that the readiness status is changed to UP once the user deployment checks (which there are none
        // in this case) are processed
        final CompletableFuture<Void> testResultFuture = CompletableFuture.runAsync(() -> {
            boolean connectionEstablished = false;

            do {
                try {
                    checkGlobalOutcome(managementClient, "check-ready", false, null);
                    connectionEstablished = true;
                } catch (ConnectException ce) {
                    // OK, server is not started yet
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                } catch (InvalidHealthResponseException ihre) {
                    // this might happen if the first call hits already processed user checks (responds UP)
                    // so continue to check if the rest of the responses is correct
                    connectionEstablished = true;
                }

            } while (!connectionEstablished);

            boolean userChecksProcessed = false;

            do {
                try {
                    checkGlobalOutcome(managementClient, "check-ready", false, null);
                } catch (InvalidHealthResponseException ihre) {
                    // OK user checks are processed, check once more that we have an UP empty response
                    userChecksProcessed = true;
                    try {
                        checkGlobalOutcome(managementClient, "check-ready", true, null);
                    } catch (IOException | InvalidHealthResponseException ex) {
                        throw new RuntimeException(ex);
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            } while (!userChecksProcessed);
        });

        Map<String, String> map = getVMArgMap(DISABLE_DEFAULT_PROCEDURES_PROPERTY);
        containerController.start(CONTAINER_NAME, map);

        try {
            testResultFuture.get(1, TimeUnit.MINUTES);
        } finally {
            deployer.undeploy(MICRO_PROFILE_HEALTH_APPLICATION_WITHOUT_READINESS_TEST_BASE);
            containerController.stop(CONTAINER_NAME);
        }

    }

    @Test
    @InSequence(3)
    public void testApplicationReadinessWithDeploymentContainingReadinessCheckWithDefaultEmptyReadiness() throws Exception {

        // deploy the application and stop the container
        containerController.start(CONTAINER_NAME);
        deployer.deploy(MICRO_PROFILE_HEALTH_APPLICATION_WITH_SUCCESSFUL_READINESS_TEST_BASE);
        containerController.stop(CONTAINER_NAME);

        // check that the readiness status is changed to UP once the user deployment checks are processed
        final CompletableFuture<Void> testResultFuture = CompletableFuture.runAsync(() -> {
            boolean connectionEstablished = false;

            do {
                try {
                    checkGlobalOutcome(managementClient, "check-ready", false, null);
                    connectionEstablished = true;
                } catch (ConnectException ce) {
                    // OK, server is not started yet
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                } catch (InvalidHealthResponseException ihre) {
                    // this might happen if the first call hits already processed user checks (responds UP)
                    // so continue to check if the rest of the responses is correct
                    connectionEstablished = true;
                }

            } while (!connectionEstablished);

            boolean userChecksProcessed = false;

            do {
                try {
                    checkGlobalOutcome(managementClient, "check-ready", false, null);
                } catch (InvalidHealthResponseException ihre) {
                    // OK user checks are processed, check once more that we have an UP response
                    userChecksProcessed = true;
                    try {
                        checkGlobalOutcome(managementClient, "check-ready", true,
                            SuccessfulReadinessCheck.NAME);
                    } catch (IOException | InvalidHealthResponseException ex) {
                        throw new RuntimeException(ex);
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            } while (!userChecksProcessed);
        });

        Map<String, String> map = getVMArgMap(DISABLE_DEFAULT_PROCEDURES_PROPERTY);
        containerController.start(CONTAINER_NAME, map);

        try {
            testResultFuture.get(1, TimeUnit.MINUTES);
        } finally {
            deployer.undeploy(MICRO_PROFILE_HEALTH_APPLICATION_WITH_SUCCESSFUL_READINESS_TEST_BASE);
            containerController.stop(CONTAINER_NAME);
        }
    }

    @Test
    @InSequence(4)
    public void testApplicationReadinessWithoutDeploymentWithEmptyReadinessSetToUP() throws Exception {

        final CompletableFuture<Void> testResultFuture = CompletableFuture.runAsync(() -> {
            boolean connectionEstablished = false;

            do {
                try {
                    checkGlobalOutcome(managementClient, "check-ready", true, null);
                    connectionEstablished = true;
                } catch (ConnectException ce) {
                    // OK, server is not started yet
                } catch (IOException | InvalidHealthResponseException ex) {
                    throw new RuntimeException(ex);
                }

            } while (!connectionEstablished);

            try {
                checkGlobalOutcome(managementClient, "check-ready", true, null);
            } catch (IOException | InvalidHealthResponseException ex) {
                throw new RuntimeException(ex);
            }
        });

        Map<String, String> map = getVMArgMap(String.format("%s %s=UP",
            DISABLE_DEFAULT_PROCEDURES_PROPERTY, DEFAULT_READINESS_EMPTY_RESPONSE_PROPERTY));
        containerController.start(CONTAINER_NAME, map);

        try {
            testResultFuture.get(1, TimeUnit.MINUTES);
        } finally {
            containerController.stop(CONTAINER_NAME);
        }
    }

    private Map<String, String> getVMArgMap(String vmArgs) {
        Map<String, String> map = new HashMap<>();
        map.put(JAVA_VM_ARGUMENTS, System.getProperty(MICROPROFILE_SERVER_JVM_ARGS) + " " + vmArgs);
        return map;
    }
}
