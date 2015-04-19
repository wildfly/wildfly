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

package org.jboss.as.test.manualmode.deployment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author wangchao
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class FileSystemDeploymentRedeployTestCase {

    private static final String DEPLOYMENT_NAME = "deployment.war";
    private static final String DEPLOYED = ".deployed";
    private static final String FAILED_DEPLOY = ".failed";

    private static final String DEFAULT_SERVER = "standalone";

    private static final String CONTAINER = "default-jbossas";

    private static Logger LOGGER = Logger.getLogger(FileSystemDeploymentRedeployTestCase.class);

    @ArquillianResource
    private static ContainerController containerController;

    private File deployDir;
    private File deployment;
    private File deployedMarker;
    private File failedMarker;

    @Before
    public void before() throws Exception {
        String jbossHome = System.getProperty("jboss.home");
        deployDir = new File(jbossHome + File.separator + DEFAULT_SERVER + File.separator + "deployments");
        assertTrue("Unable to find deployment scanner directory.", deployDir.exists());

        deployedMarker = new File(deployDir, DEPLOYMENT_NAME + DEPLOYED);
        failedMarker = new File(deployDir, DEPLOYMENT_NAME + FAILED_DEPLOY);

        LOGGER.info("*** starting server");
        containerController.start(CONTAINER);
    }

    @After
    public void after() throws Exception {
        FileUtils.cleanDirectory(deployDir);
        LOGGER.info("*** stopping server");
        containerController.stop(CONTAINER);
    }

    private void createDeployment(final File file) throws IOException {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class);
        archive.setWebXML(FileSystemDeploymentRedeployTestCase.class.getPackage(), "web.xml");
        archive.as(ZipExporter.class).exportTo(file, true);
    }
    
    private void createBrokenDeployment(final File file) throws IOException {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class);
        archive.addAsWebInfResource(new StringAsset("Malformed web.xml"), "web.xml");
        archive.as(ZipExporter.class).exportTo(file, true);
    }

    /**
     * WFCORE-505 Test FileSystemDeployment scanner redundant re-deployment duplicate resource error when .failed marker exists
     * and new archive file contains newer time-stamp
     *
     * @throws Exception
     */
    @Test
    public void testFileSystemDeploymentRedeploy() throws Exception {
        // first create broken deployment.war, it will generate .failed marker;
        deployment = new File(deployDir, DEPLOYMENT_NAME);
        createBrokenDeployment(deployment);

        // wait for scan-interval
        TimeUnit.MILLISECONDS.sleep(6000);

        assertTrue("deployment file is expected", deployment.exists());
        assertFalse(".deployed marker is expected", deployedMarker.exists());
        assertTrue(".failed marker is unexpected", failedMarker.exists());

        LOGGER.info("*** stopping server");
        containerController.stop(CONTAINER);

        // replace broken deployment archive with a good one
        createDeployment(deployment);

        LOGGER.info("*** starting server");
        containerController.start(CONTAINER);

        // wait for scan-interval
        TimeUnit.MILLISECONDS.sleep(6000);

        assertTrue("deployment file is expected", deployment.exists());
        assertTrue(".deployed marker is expected", deployedMarker.exists());
        assertFalse(".failed marker is unexpected", failedMarker.exists());
    }
}
