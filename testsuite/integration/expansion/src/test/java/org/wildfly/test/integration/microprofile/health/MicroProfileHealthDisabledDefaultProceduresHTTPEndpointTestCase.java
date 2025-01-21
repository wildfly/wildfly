/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import org.jboss.as.arquillian.container.ManagementClient;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.wildfly.test.integration.microprofile.health.MicroProfileHealthUtils.testHttpEndPoint;

public class MicroProfileHealthDisabledDefaultProceduresHTTPEndpointTestCase extends
        MicroProfileHealthDisabledDefaultProceduresTestBase {

    @Override
    void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName, Integer expectedChecksCount) throws IOException {
        assertEquals("check", operation);
        final String httpEndpoint = "/health";
        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + httpEndpoint;
        testHttpEndPoint(healthURL, mustBeUP, probeName, expectedChecksCount);
    }
}
