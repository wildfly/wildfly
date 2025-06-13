/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.faulttolerance.multideployment;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.faulttolerance.micrometer.FaultToleranceMicrometerIntegrationTestCase;
import org.wildfly.test.integration.microprofile.faulttolerance.micrometer.deployment.FaultTolerantApplication;

/**
 * Test that reuses existing {@link FaultTolerantApplication} application which deploys twice simultaneously with
 * Micrometer metrics enabled.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(MicrometerSetupTask.class)
@TestcontainersRequired
@org.junit.Ignore
public class MultipleDeploymentMetricsTestCase {

    public static final String DEPLOYMENT_1 = "deployment-1";
    public static final String DEPLOYMENT_2 = "deployment-2";

    @Deployment(name = DEPLOYMENT_1, testable = false)
    public static Archive<?> deployment1() {
        return deployment(1);
    }

    @Deployment(name = DEPLOYMENT_2, testable = false)
    public static Archive<?> deployment2() {
        return deployment(2);
    }

    public static Archive<?> deployment(int deploymentSerial) {
        return ShrinkWrap.create(WebArchive.class, MultipleDeploymentMetricsTestCase.class.getSimpleName() + "-" + deploymentSerial + ".war")
                .addClasses(ServerSetupTask.class)
                .addPackage(FaultTolerantApplication.class.getPackage())
                .addAsWebInfResource(FaultToleranceMicrometerIntegrationTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(FaultToleranceMicrometerIntegrationTestCase.class.getPackage(), "beans.xml", "beans.xml");
    }

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_1)
    private URL url1;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_2)
    private URL url2;

    @Test
    @RunAsClient
    public void makeRequests() throws IOException, ExecutionException, TimeoutException {
        String requestUrl1 = url1.toString() + "app/timeout";
        String requestUrl2 = url2.toString() + "app/timeout";

        for (int i = 0; i < 5; i++) {
            HttpRequest.get(requestUrl1, 10, TimeUnit.SECONDS);
            HttpRequest.get(requestUrl2, 10, TimeUnit.SECONDS);
        }
    }

}
