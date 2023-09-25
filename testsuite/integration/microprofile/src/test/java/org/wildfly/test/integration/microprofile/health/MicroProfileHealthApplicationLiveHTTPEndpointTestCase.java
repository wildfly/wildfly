/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import java.io.IOException;

import org.jboss.as.arquillian.container.ManagementClient;
import static org.wildfly.test.integration.microprofile.health.MicroProfileHealthUtils.testHttpEndPoint;


/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MicroProfileHealthApplicationLiveHTTPEndpointTestCase extends MicroProfileHealthApplicationLiveTestBase {

    void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName) throws IOException {

        final String httpEndpoint;
        switch(operation) {
            case "check-live":
                httpEndpoint = "/health/live";
                break;
            case "check-ready":
                httpEndpoint = "/health/ready";
                break;
            case "check":
            default:
                httpEndpoint = "/health";
        }
        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + httpEndpoint;
        testHttpEndPoint(healthURL, mustBeUP, probeName);
    }
}
