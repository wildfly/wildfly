/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import org.jboss.as.arquillian.container.ManagementClient;

import java.io.IOException;

import static org.wildfly.test.integration.microprofile.health.MicroProfileHealthUtils.retrieveHttpEndPointChecks;

public class MicroProfileHealthDisabledDefaultProceduresHTTPEndpointTestCase extends MicroProfileHealthDisabledDefaultProceduresTestBase {

    @Override
    protected Integer retrieveChecks(ManagementClient managementClient, final String checkType) throws IOException {

        final String httpEndpoint = "/health/" + checkType;

        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + httpEndpoint;

        return retrieveHttpEndPointChecks(healthURL);
    }
}
