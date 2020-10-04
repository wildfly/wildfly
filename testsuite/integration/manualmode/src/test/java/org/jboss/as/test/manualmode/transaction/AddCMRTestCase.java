/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
