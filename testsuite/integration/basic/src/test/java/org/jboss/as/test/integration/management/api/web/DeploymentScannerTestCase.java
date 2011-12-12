/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.api.web;

import org.junit.After;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.cli.GlobalOpsTestCase;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DeploymentScannerTestCase extends AbstractMgmtTestBase {

    private static final String tempDir = System.getProperty("java.io.tmpdir");
    private static WebArchive war;
    private static File warFile;
    private static File deployDir;

    @ArquillianResource
    URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(DeploymentScannerTestCase.class);
        return ja;
    }

    @Before
    public void beforeClass() throws IOException {
        initModelControllerClient(url.getHost(), MGMT_PORT);
    }

    @AfterClass
    public static void afterClass() throws IOException {
        closeModelControllerClient();
    }

    @Before
    public void before() throws IOException {
        deployDir = new File(tempDir + File.separator + "tempDeployment");
        if (deployDir.exists()) {
            FileUtils.deleteDirectory(deployDir);
        }
        assertTrue("Unable to create deployment scanner directory.", deployDir.mkdir());
    }

    @After
    public void after() throws IOException {
        FileUtils.deleteDirectory(deployDir);
    }

    @Test
    public void testAddRemove() throws Exception {

        prepareDeployment();
        addDeploymentScanner();
        removeDeploymentScanner();
    }

    @Test
    public void testAddWrongPath() throws Exception {

        prepareDeployment();

        // add DS with non existing path
        ModelNode op = createOpNode("subsystem=deployment-scanner/scanner=testScanner", "add");
        op.get("scan-interval").set(2000);
        op.get("path").set("/tmp/DeploymentScannerTestCase/nonExistingPath");

        ModelNode ret = executeOperation(op, false);
        // check that it failed
        assertTrue("failed".equals(ret.get("outcome").asString()));

        // check that rollback was success and we can create propper one
        addDeploymentScanner();
        removeDeploymentScanner();

    }

    @Test
    public void testAddRemoveRollbacks() throws Exception {

        prepareDeployment();

        // execute and rollback add deployment scanner
        ModelNode addOp = getAddDSOp();
        ModelNode ret = executeAndRollbackOperation(addOp);
        assertTrue("failed".equals(ret.get("outcome").asString()));

        // add deployment scanner
        addDeploymentScanner();

        // execute and rollback remove deployment scanner
        ModelNode removeOp = getRemoveDSOp();
        ret = executeAndRollbackOperation(removeOp);
        assertTrue("failed".equals(ret.get("outcome").asString()));

        // check that the ds is still present


        // remove deployment scanner
        removeDeploymentScanner();
    }

    private void addDeploymentScanner() throws Exception {

        // add deployment scanner
        ModelNode op = getAddDSOp();
        executeOperation(op);

        // wait for deployment
        Thread.sleep(2000);

        // check that the app has been deployed
        File marker = new File(deployDir.getAbsolutePath() + File.separator + "SimpleServlet.war.deployed");
        assertTrue(marker.exists());

        String response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/SimpleServlet", 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("SimpleServlet") >=0);

    }

    private void removeDeploymentScanner() throws Exception {

        // remove deployment scanner
        executeOperation(getRemoveDSOp());

        // delete deployment
        assertTrue("Could not delete deployed file.", warFile.delete());

        // wait for deployment
        Thread.sleep(2000);

        // check that the deployment is still live
        String response = HttpRequest.get(getBaseURL(url) + "SimpleServlet/SimpleServlet", 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("SimpleServlet") >=0);

        // undeploy
        ModelNode op = createOpNode("deployment=SimpleServlet.war", "remove");
        executeOperation(op);

    }

    private ModelNode getAddDSOp() {
        String path =  deployDir.getAbsolutePath();
        path = path.replaceAll("\\\\", "/");

        ModelNode op = createOpNode("subsystem=deployment-scanner/scanner=testScanner", "add");
        op.get("scan-interval").set(2000);
        op.get("path").set(path);
        return op;
    }

    private ModelNode getRemoveDSOp() {
        return createOpNode("subsystem=deployment-scanner/scanner=testScanner", "remove");
    }

    private void prepareDeployment() {
        war = ShrinkWrap.create(WebArchive.class, "SimpleServlet.war");
        war.addClass(SimpleServlet.class);
        warFile = new File(deployDir.getAbsolutePath() + File.separator + "SimpleServlet.war");
        new ZipExporterImpl(war).exportTo(warFile, true);
    }
}
