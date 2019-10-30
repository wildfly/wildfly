/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.DataSourceTestServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests a DataSource which uses Credentials stored in a security domain.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SecuredDataSourceTestCase extends AbstractCliTestBase {

    private static final String CONTAINER = "default-jbossas";
    private static final String DEPLOYMENT = "deployment";

    private static final String TEST_NAME = SecuredDataSourceTestCase.class.getSimpleName();

    private static final String BATCH_CLI_FILENAME = TEST_NAME + ".cli";
    private static final String REMOVE_BATCH_CLI_FILENAME1 = TEST_NAME + "-remove.cli";
    private static final String REMOVE_BATCH_CLI_FILENAME2 = TEST_NAME + "-remove2.cli";

    private static final File WORK_DIR = new File("secured-ds-" + System.currentTimeMillis());
    private static final File BATCH_CLI_FILE = new File(WORK_DIR, BATCH_CLI_FILENAME);
    private static final File REMOVE_BATCH_CLI_FILE1 = new File(WORK_DIR, REMOVE_BATCH_CLI_FILENAME1);
    private static final File REMOVE_BATCH_CLI_FILE2 = new File(WORK_DIR, REMOVE_BATCH_CLI_FILENAME2);

    @ArquillianResource
    private static ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    @Test
    public void test() throws Exception {
        final URI uri = new URI("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/" + TEST_NAME
                + DataSourceTestServlet.SERVLET_PATH + "?" + DataSourceTestServlet.PARAM_DS + "=" + TEST_NAME);
        final String body = Utils.makeCall(uri, HttpServletResponse.SC_OK);
        assertEquals("true", body);
    }

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, TEST_NAME + ".war");
        war.addClass(DataSourceTestServlet.class);
        return war;
    }

    /**
     * Configure the AS and LDAP as the first step in this testcase.
     *
     * @throws Exception
     */
    @Test
    @InSequence(Integer.MIN_VALUE)
    public void initServer() throws Exception {
        container.start(CONTAINER);

        WORK_DIR.mkdirs();

        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(BATCH_CLI_FILENAME), BATCH_CLI_FILE);
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(REMOVE_BATCH_CLI_FILENAME1), REMOVE_BATCH_CLI_FILE1);
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(REMOVE_BATCH_CLI_FILENAME2), REMOVE_BATCH_CLI_FILE2);

        initCLI();
        final boolean batchResult = runBatch(BATCH_CLI_FILE);
        closeCLI();

        try {
            assertTrue("Server configuration failed", batchResult);
        } finally {
            container.stop(CONTAINER);
        }
        container.start(CONTAINER);

        deployer.deploy(DEPLOYMENT);

    }

    /**
     * Revert the AS configuration and stop the server as the last but one step.
     *
     * @throws Exception
     */
    @Test
    @InSequence(Integer.MAX_VALUE)
    public void closeServer() throws Exception {
        assertTrue(container.isStarted(CONTAINER));

        deployer.undeploy(DEPLOYMENT);

        initCLI();
        boolean batchResult = runBatch(REMOVE_BATCH_CLI_FILE1);
        //server reload
        container.stop(CONTAINER);
        container.start(CONTAINER);
        batchResult = batchResult && runBatch(REMOVE_BATCH_CLI_FILE2);
        closeCLI();
        container.stop(CONTAINER);

        FileUtils.deleteQuietly(WORK_DIR);

        assertTrue("Reverting server configuration failed", batchResult);
    }

    /**
     * Runs given CLI script file as a batch. The CLI has to be initialized before calling this method.
     *
     * @param batchFile CLI file to run in batch
     * @return true if CLI returns Success
     * @throws IOException
     */
    protected static boolean runBatch(File batchFile) throws IOException {
        cli.sendLine("run-batch --file=\"" + batchFile.getAbsolutePath()
                + "\" --headers={allow-resource-service-restart=true} -v", false);
        return cli.readAllAsOpResult().isIsOutcomeSuccess();
    }
}
