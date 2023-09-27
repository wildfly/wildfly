/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.transaction;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@RunAsClient
public class AddCMRTestCase extends AbstractCliTestBase {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(AddCMRTestCase.class);

    private static final String CONTAINER = "default-jbossas";
    private static final String CMR_CLI_CMD = "/subsystem=transactions/commit-markable-resource=\"java:jboss/datasources/test-cmr\"";

    @ArquillianResource
    private static ContainerController container;

    @Before
    public void before() throws Exception {
        if (!container.isStarted(CONTAINER)) {
            container.start(CONTAINER);
        }

        initCLI(TimeoutUtil.adjust(20 * 1000));
    }

    @After
    public void after() throws Exception {
        if (container.isStarted(CONTAINER)) {
            container.stop(CONTAINER);
        }

        closeCLI();
    }

    @Test
    public void testAddCMR() throws IOException {
        cli.sendLine(CMR_CLI_CMD + ":add()");

        CLIOpResult result = cli.readAllAsOpResult();

        assertNotNull("Failed to add CMR datasource (null CLIOpResult).", result);
        assertTrue("Failed to add CMR datasource.", result.isIsOutcomeSuccess());

        try {
            if (result.getFromResponse(RESPONSE_HEADERS) != null) {
                assertEquals("restart-required",
                        ((Map) result.getFromResponse(RESPONSE_HEADERS)).get(PROCESS_STATE));
            }
        } finally {
            cli.sendLine(CMR_CLI_CMD + ":remove()");
        }
    }
}
