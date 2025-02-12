/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import org.jboss.as.arquillian.container.ManagementClient;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.wildfly.test.integration.microprofile.health.MicroProfileHealthUtils.testHttpEndPoint;

/**
 * @author <a href="http://xstefank.io/">Martin Stefanko</a> (c) 2021 Red Hat inc.
 */
public class MicroProfileHealthApplicationStartupHTTPEndpointTestCase extends MicroProfileHealthApplicationStartupTestBase {

    void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName) throws IOException {

        assertEquals("check-started", operation);
        final String httpEndpoint = "/health/started";

        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + httpEndpoint;
        testHttpEndPoint(healthURL, mustBeUP, probeName);
    }
}
