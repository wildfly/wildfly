/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.jboss.as.arquillian.container.ManagementClient;
import static org.wildfly.test.integration.microprofile.health.MicroProfileHealthUtils.testHttpEndPoint;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2019 Red Hat inc.
 */
public class MicroProfileHealthApplicationWithoutReadinessHTTPEndpointTestCase extends MicroProfileHealthApplicationWithoutReadinessTestBase {

    void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName) throws IOException {

        assertEquals("check-ready", operation);
        final String httpEndpoint = "/health/ready";

        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + httpEndpoint;
        testHttpEndPoint(healthURL, mustBeUP, probeName);
    }
}
