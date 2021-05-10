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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.jca.statistics;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;

/**
 * Tests the data source statistics attribute MaxWaitCount during waiting requests.
 *
 * Test for [ WFLY-14691 ].
 *
 * @author Daniel Cihak
 *
 */
@RunWith(Arquillian.class)
@ServerSetup(DataSourceMultipleConnStatsServerSetupTask.class)
@RunAsClient
public class DataSourceMultipleConnStatsTestCase {

    private static final String DEPLOYMENT = "DS_STATISTICS";
    static final Logger LOGGER = Logger.getLogger(DataSourceMultipleConnStatsTestCase.class);

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(name = DEPLOYMENT)
    public static WebArchive appDeployment1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClass(SleepServlet.class);
        war.addAsManifestResource(new StringAsset("Dependencies: com.h2database.h2\n"),"MANIFEST.MF");
        return war;
    }

    @Test
    public void testDataSourceStatistics(@ArquillianResource URL url) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        Runnable sleepServletCall = () -> {
            try {
                LOGGER.debug("About to call from " + Thread.currentThread().getName());
                String response = HttpRequest.get(url.toExternalForm() + "sleep/", 30, SECONDS);
                LOGGER.debug("Finished call from " + Thread.currentThread().getName());
                Assert.assertTrue("Unexpected message from the servlet.", response.contains("Servlet result OK"));
            } catch (Exception e) {
                throw new RuntimeException("Servlet request processing failed due to " + e.getMessage());
            }
        };

        int maxWaitCount = readAttribute("MaxWaitCount").asInt();
        assertEquals(0, maxWaitCount);

        try {
            executor.execute(sleepServletCall);

            Thread.sleep(1000L);

            executor.execute(sleepServletCall);

            Thread.sleep(1000L);

            maxWaitCount = readAttribute("MaxWaitCount").asInt();
            assertEquals(1, maxWaitCount);

            executor.execute(sleepServletCall);

            Thread.sleep(1000L);

            maxWaitCount = readAttribute("MaxWaitCount").asInt();
            assertEquals(2, maxWaitCount);
        } finally {
            executor.shutdown();
        }
    }

    // /subsystem=datasources/data-source=ExampleDS/statistics=pool:read-attribute(name=AvailableCount)
    private ModelNode readAttribute(String attributeName) throws Exception {
        ModelNode operation = createOpNode("subsystem=datasources/data-source=ExampleDS/statistics=pool/", READ_ATTRIBUTE_OPERATION);
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(attributeName);
        return ManagementOperations.executeOperation(managementClient.getControllerClient(), operation);
    }
}
