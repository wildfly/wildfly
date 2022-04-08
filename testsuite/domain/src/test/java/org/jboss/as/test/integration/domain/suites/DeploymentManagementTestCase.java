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

package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FULL_REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_REPLACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_STREAM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_URL;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.cleanFile;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.safeClose;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of various management operations involving deployment.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentManagementTestCase {

    private static final String TEST = "test.war";
    private static final String TEST2 = "test2.war";
    private static final String REPLACEMENT = "test.war.v2";
    private static final ModelNode ROOT_ADDRESS = new ModelNode();
    private static final ModelNode ROOT_DEPLOYMENT_ADDRESS = new ModelNode();
    private static final ModelNode ROOT_REPLACEMENT_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_SERVER_GROUP_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS = new ModelNode();
    private static final ModelNode OTHER_SERVER_GROUP_ADDRESS = new ModelNode();
    private static final ModelNode OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_RUNNING_SERVER_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_RUNNING_SERVER_DEPLOYMENT_ADDRESS = new ModelNode();
    private static final ModelNode OTHER_RUNNING_SERVER_ADDRESS = new ModelNode();
    private static final ModelNode OTHER_RUNNING_SERVER_GROUP_ADDRESS = new ModelNode();

    static {
        ROOT_ADDRESS.setEmptyList();
        ROOT_ADDRESS.protect();
        ROOT_DEPLOYMENT_ADDRESS.add(DEPLOYMENT, TEST);
        ROOT_DEPLOYMENT_ADDRESS.protect();
        ROOT_REPLACEMENT_ADDRESS.add(DEPLOYMENT, REPLACEMENT);
        ROOT_REPLACEMENT_ADDRESS.protect();
        MAIN_SERVER_GROUP_ADDRESS.add(SERVER_GROUP, "main-server-group");
        MAIN_SERVER_GROUP_ADDRESS.protect();
        MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS.add(SERVER_GROUP, "main-server-group");
        MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS.add(DEPLOYMENT, TEST);
        MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS.protect();
        OTHER_SERVER_GROUP_ADDRESS.add(SERVER_GROUP, "other-server-group");
        OTHER_SERVER_GROUP_ADDRESS.protect();
        OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS.add(SERVER_GROUP, "other-server-group");
        OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS.add(DEPLOYMENT, TEST);
        OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS.protect();
        MAIN_RUNNING_SERVER_ADDRESS.add(HOST, "master");
        MAIN_RUNNING_SERVER_ADDRESS.add(SERVER, "main-one");
        MAIN_RUNNING_SERVER_ADDRESS.protect();
        MAIN_RUNNING_SERVER_DEPLOYMENT_ADDRESS.add(HOST, "master");
        MAIN_RUNNING_SERVER_DEPLOYMENT_ADDRESS.add(SERVER, "main-one");
        MAIN_RUNNING_SERVER_DEPLOYMENT_ADDRESS.add(DEPLOYMENT, TEST);
        MAIN_RUNNING_SERVER_DEPLOYMENT_ADDRESS.protect();
        OTHER_RUNNING_SERVER_ADDRESS.add(HOST, "slave");
        OTHER_RUNNING_SERVER_ADDRESS.add(SERVER, "other-two");
        OTHER_RUNNING_SERVER_ADDRESS.protect();
        OTHER_RUNNING_SERVER_GROUP_ADDRESS.add(HOST, "slave");
        OTHER_RUNNING_SERVER_GROUP_ADDRESS.add(SERVER, "other-two");
        OTHER_RUNNING_SERVER_GROUP_ADDRESS.add(DEPLOYMENT, TEST);
        OTHER_RUNNING_SERVER_GROUP_ADDRESS.protect();

    }

    private static DomainTestSupport testSupport;
    private static WebArchive webArchive;
    private static WebArchive webArchive2;
    private static File tmpDir;

    @BeforeClass
    public static void setupDomain() throws Exception {

        // Create our deployments
        webArchive = ShrinkWrap.create(WebArchive.class, TEST);
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL index = tccl.getResource("helloWorld/index.html");
        webArchive.addAsWebResource(index, "index.html");

        webArchive2 = ShrinkWrap.create(WebArchive.class, TEST);
        index = tccl.getResource("helloWorld/index.html");
        webArchive2.addAsWebResource(index, "index.html");
        index = tccl.getResource("helloWorld/index2.html");
        webArchive2.addAsWebResource(index, "index2.html");

        // Make versions on the filesystem for URL-based deploy and for unmanaged content testing
        tmpDir = new File("target/deployments/" + DeploymentManagementTestCase.class.getSimpleName());
        new File(tmpDir, "archives").mkdirs();
        new File(tmpDir, "exploded").mkdirs();
        webArchive.as(ZipExporter.class).exportTo(new File(tmpDir, "archives/" + TEST), true);
        webArchive.as(ExplodedExporter.class).exportExploded(new File(tmpDir, "exploded"));

        // Launch the domain
        testSupport = DomainTestSuite.createSupport(DeploymentManagementTestCase.class.getSimpleName());
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        try {
            testSupport = null;
            DomainTestSuite.stopSupport();
        } finally {
            cleanFile(tmpDir);
        }
    }

    /**
     * Validate that there are no deployments; try and clean if there are.
     *
     * @throws Exception
     */
    @Before
    @After
    public void confirmNoDeployments() throws Exception {
        List<ModelNode> deploymentList = getDeploymentList(ROOT_ADDRESS);
        if (deploymentList.size() > 0) {
            cleanDeployments();
        }
        deploymentList = getDeploymentList(new ModelNode());
        assertEquals("Deployments are removed from the domain", 0, deploymentList.size());

        try {
            performHttpCall(DomainTestSupport.masterAddress, 8080);
            fail(TEST + " is available on main-one");
        } catch (IOException good) {
            // good
        }
        try {
            performHttpCall(DomainTestSupport.slaveAddress, 8630);
            fail(TEST + " is available on other-three");
        } catch (IOException good) {
            // good
        }
    }

    /**
     * Remove all deployments from the model.
     *
     * @throws IOException
     */
    private void cleanDeployments() throws IOException {
        List<ModelNode> deploymentList = getDeploymentList(MAIN_SERVER_GROUP_ADDRESS);
        for (ModelNode deployment : deploymentList) {
            removeDeployment(deployment.asString(), MAIN_SERVER_GROUP_ADDRESS);
        }
        deploymentList = getDeploymentList(OTHER_SERVER_GROUP_ADDRESS);
        for (ModelNode deployment : deploymentList) {
            removeDeployment(deployment.asString(), OTHER_SERVER_GROUP_ADDRESS);
        }
        deploymentList = getDeploymentList(ROOT_ADDRESS);
        for (ModelNode deployment : deploymentList) {
            removeDeployment(deployment.asString(), ROOT_ADDRESS);
        }
    }


    @Test
    public void testDeploymentViaUrl() throws Exception {
        String url = new File(tmpDir, "archives/" + TEST).toURI().toURL().toString();
        ModelNode content = new ModelNode();
        content.get("url").set(url);
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        executeOnMaster(composite);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testDeploymentViaStream() throws Exception {
        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        OperationBuilder builder = new OperationBuilder(composite, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }


    @Test
    public void testUploadURL() throws Exception {
        String url = new File(tmpDir, "archives/" + TEST).toURI().toURL().toString();
        ModelNode op = getEmptyOperation(UPLOAD_DEPLOYMENT_URL, ROOT_ADDRESS);
        op.get("url").set(url);

        byte[] hash = executeOnMaster(op).asBytes();

        testDeploymentViaHash(hash);
    }


    @Test
    public void testUploadStream() throws Exception {
        ModelNode op = getEmptyOperation(UPLOAD_DEPLOYMENT_STREAM, ROOT_ADDRESS);
        op.get(INPUT_STREAM_INDEX).set(0);
        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        byte[] hash = executeOnMaster(builder.build()).asBytes();

        testDeploymentViaHash(hash);
    }

    private void testDeploymentViaHash(byte[] hash) throws Exception {
        ModelNode content = new ModelNode();
        content.get("hash").set(hash);
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        executeOnMaster(composite);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testDomainAddOnly() throws Exception {
        ModelNode op = getEmptyOperation(UPLOAD_DEPLOYMENT_STREAM, ROOT_ADDRESS);
        op.get(INPUT_STREAM_INDEX).set(0);
        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        byte[] hash = executeOnMaster(builder.build()).asBytes();

        ModelNode content = new ModelNode();
        content.get("hash").set(hash);
        ModelNode composite = createDeploymentOperation(content);
        executeOnMaster(composite);
    }

    @Test
    public void testUnmanagedArchiveDeployment() throws Exception {
        ModelNode content = new ModelNode();
        content.get("archive").set(true);
        content.get("path").set(new File(tmpDir, "archives/" + TEST).getAbsolutePath());
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        executeOnMaster(composite);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testUnmanagedExplodedDeployment() throws Exception {
        ModelNode content = new ModelNode();
        content.get("archive").set(false);
        content.get("path").set(new File(tmpDir, "exploded/" + TEST).getAbsolutePath());
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS);

        executeOnMaster(composite);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testUndeploy() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        undeployTest();
    }

    @Test
    public void testUnmanagedArchiveUndeploy() throws Exception {
        // Establish the deployment
        testUnmanagedArchiveDeployment();

        undeployTest();
    }

    @Test
    public void testUnmanagedExplodedUndeploy() throws Exception {
        // Establish the deployment
        testUnmanagedExplodedDeployment();

        undeployTest();
    }


    private void undeployTest() throws Exception {
        ModelNode op = getEmptyOperation("undeploy", OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        executeOnMaster(op);

        // Thread.sleep(1000);

        try {
            performHttpCall(DomainTestSupport.slaveAddress, 8630);
            fail("Webapp still accessible following undeploy");
        } catch (IOException good) {
            // desired result
        }
    }

    @Test
    public void testRedeploy() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        redeployTest();
    }

    @Test
    public void testUnmanagedArchiveRedeploy() throws Exception {
        // Establish the deployment
        testUnmanagedArchiveDeployment();

        redeployTest();
    }

    @Test
    public void testUnmanagedExplodedRedeploy() throws Exception {
        // Establish the deployment
        testUnmanagedExplodedDeployment();

        redeployTest();
    }

    private void redeployTest() throws IOException {
        ModelNode op = getEmptyOperation("redeploy", OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        executeOnMaster(op);

        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testReplace() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode op = createDeploymentReplaceOperation(content, MAIN_SERVER_GROUP_ADDRESS, OTHER_SERVER_GROUP_ADDRESS);
        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());

        // Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testUnmanagedArchiveReplace() throws Exception {
        // Establish the deployment
        testUnmanagedArchiveDeployment();

        ModelNode content = new ModelNode();
        content.get("archive").set(true);
        content.get("path").set(new File(tmpDir, "archives/" + TEST).getAbsolutePath());
        ModelNode op = createDeploymentReplaceOperation(content, MAIN_SERVER_GROUP_ADDRESS, OTHER_SERVER_GROUP_ADDRESS);

        executeOnMaster(op);

        // Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testUnmanagedExplodedReplace() throws Exception {
        // Establish the deployment
        testUnmanagedArchiveDeployment();

        ModelNode content = new ModelNode();
        content.get("archive").set(false);
        content.get("path").set(new File(tmpDir, "exploded/" + TEST).getAbsolutePath());
        ModelNode op = createDeploymentReplaceOperation(content, MAIN_SERVER_GROUP_ADDRESS, OTHER_SERVER_GROUP_ADDRESS);

        executeOnMaster(op);

        // Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testUnmanagedArchiveReplaceManaged() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        ModelNode content = new ModelNode();
        content.get("archive").set(true);
        content.get("path").set(new File(tmpDir, "archives/" + TEST).getAbsolutePath());
        ModelNode op = createDeploymentReplaceOperation(content, MAIN_SERVER_GROUP_ADDRESS, OTHER_SERVER_GROUP_ADDRESS);

        executeOnMaster(op);

        // Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testUnmanagedExplodedReplaceManaged() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        ModelNode content = new ModelNode();
        content.get("archive").set(false);
        content.get("path").set(new File(tmpDir, "exploded/" + TEST).getAbsolutePath());
        ModelNode op = createDeploymentReplaceOperation(content, MAIN_SERVER_GROUP_ADDRESS, OTHER_SERVER_GROUP_ADDRESS);

        executeOnMaster(op);

        //Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testManagedReplaceUnmanaged() throws Exception {
        // Establish the deployment
        testUnmanagedArchiveDeployment();

        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode op = createDeploymentReplaceOperation(content, MAIN_SERVER_GROUP_ADDRESS, OTHER_SERVER_GROUP_ADDRESS);
        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());

        // Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }
    @Test
    public void testFullReplaceViaStream() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode op = createDeploymentFullReplaceOperation(content);
        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());

        // Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }


    @Test
    public void testFullReplaceViaUrl() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        String url = new File(tmpDir, "archives/" + TEST).toURI().toURL().toString();
        ModelNode content = new ModelNode();
        content.get("url").set(url);
        ModelNode op = createDeploymentFullReplaceOperation(content);

        executeOnMaster(op);

        //Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testFullReplaceViaHash() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();
        byte[] original = getHash(ROOT_DEPLOYMENT_ADDRESS);

        String url = new File(tmpDir, "archives/" + TEST).toURI().toURL().toString();
        ModelNode op = getEmptyOperation(UPLOAD_DEPLOYMENT_URL, ROOT_ADDRESS);
        op.get("url").set(url);

        byte[] hash = executeOnMaster(op).asBytes();

        ModelNode content = new ModelNode();
        content.get("hash").set(hash);
        op = createDeploymentFullReplaceOperation(content);

        executeOnMaster(op);

        // Check that the original content got removed!
        testRemovedContent(testSupport.getDomainMasterLifecycleUtil(), original);
        testRemovedContent(testSupport.getDomainSlaveLifecycleUtil(), original);

        //Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testFullReplaceDifferentFile() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode op = createDeploymentFullReplaceOperation(content);
        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(webArchive2.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());

        //Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }


    @Test
    public void testUnmanagedArchiveFullReplace() throws Exception {
        // Establish the deployment
        testUnmanagedArchiveDeployment();

        ModelNode content = new ModelNode();
        content.get("archive").set(true);
        content.get("path").set(new File(tmpDir, "archives/" + TEST).getAbsolutePath());
        ModelNode op = createDeploymentFullReplaceOperation(content);

        executeOnMaster(op);

        //Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testUnmanagedExplodedFullReplace() throws Exception {
        // Establish the deployment
        testUnmanagedExplodedDeployment();

        ModelNode content = new ModelNode();
        content.get("archive").set(false);
        content.get("path").set(new File(tmpDir, "exploded/" + TEST).getAbsolutePath());
        ModelNode op = createDeploymentFullReplaceOperation(content);

        executeOnMaster(op);

        //Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testUnmanagedArchiveFullReplaceManaged() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        ModelNode content = new ModelNode();
        content.get("archive").set(true);
        content.get("path").set(new File(tmpDir, "archives/" + TEST).getAbsolutePath());
        ModelNode op = createDeploymentFullReplaceOperation(content);

        executeOnMaster(op);

        //Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testUnmanagedExplodedFullReplaceManaged() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        ModelNode content = new ModelNode();
        content.get("archive").set(false);
        content.get("path").set(new File(tmpDir, "exploded/" + TEST).getAbsolutePath());
        ModelNode op = createDeploymentFullReplaceOperation(content);

        executeOnMaster(op);

        //Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testManagedFullReplaceUnmanaged() throws Exception {
        // Establish the deployment
        testUnmanagedExplodedDeployment();

        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode op = createDeploymentFullReplaceOperation(content);
        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());

        //Thread.sleep(1000);

        performHttpCall(DomainTestSupport.masterAddress, 8080);
        performHttpCall(DomainTestSupport.slaveAddress, 8630);
    }

    @Test
    public void testServerGroupRuntimeName() throws Exception {
        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode composite = createDeploymentOperation(content, OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        // Chnage the runtime name in the sg op
        composite.get("steps").get(1).get(RUNTIME_NAME).set("test1.war");

        OperationBuilder builder = new OperationBuilder(composite, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());
        performHttpCall(DomainTestSupport.slaveAddress, 8630, "test1");
    }

    @Test
    public void testDeployToSingleServerGroup() throws Exception {
        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode composite = createDeploymentOperation(content, OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        OperationBuilder builder = new OperationBuilder(composite, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());

        performHttpCall(DomainTestSupport.slaveAddress, 8630);
        try {
            performHttpCall(DomainTestSupport.masterAddress, 8080);
            fail("Webapp deployed to unselected server group");
        } catch (IOException ioe) {
            // good
        }
    }

    @Test
    public void testDeploymentsWithSameHash() throws Exception {
        final ModelNode rootDeploymentAddress2 = new ModelNode();
        rootDeploymentAddress2.add(DEPLOYMENT, "test2");
        rootDeploymentAddress2.protect();

        final ModelNode otherServerGroupDeploymentAddress2 = new ModelNode();
        otherServerGroupDeploymentAddress2.add(SERVER_GROUP, "other-server-group");
        otherServerGroupDeploymentAddress2.add(DEPLOYMENT, "test2");
        otherServerGroupDeploymentAddress2.protect();

        class LocalMethods {
            Operation createDeploymentOperation(ModelNode rootDeploymentAddress, ModelNode serverGroupDeploymentAddress) {
                ModelNode composite = getEmptyOperation(COMPOSITE, ROOT_ADDRESS);
                ModelNode steps = composite.get(STEPS);

                ModelNode step = steps.add();
                step.set(getEmptyOperation(ADD, rootDeploymentAddress));
                ModelNode content = new ModelNode();
                content.get(INPUT_STREAM_INDEX).set(0);
                step.get(CONTENT).add(content);

                step = steps.add();
                step.set(getEmptyOperation(ADD, serverGroupDeploymentAddress));
                step.get(ENABLED).set(true);

                OperationBuilder builder = new OperationBuilder(composite, true);
                builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());
                return builder.build();
             }

             ModelNode createRemoveOperation(ModelNode rootDeploymentAddress, ModelNode serverGroupDeploymentAddress) {
                 ModelNode composite = getEmptyOperation(COMPOSITE, ROOT_ADDRESS);
                 ModelNode steps = composite.get(STEPS);

                 ModelNode step = steps.add();
                 step.set(getEmptyOperation(REMOVE, serverGroupDeploymentAddress));

                 step = steps.add();
                 step.set(getEmptyOperation(REMOVE, rootDeploymentAddress));

                 return composite;
             }

        }
        LocalMethods localMethods = new LocalMethods();
        try {
            executeOnMaster(localMethods.createDeploymentOperation(ROOT_DEPLOYMENT_ADDRESS, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS));
            try {
                executeOnMaster(localMethods.createDeploymentOperation(rootDeploymentAddress2, otherServerGroupDeploymentAddress2));
            } finally {
                executeOnMaster(localMethods.createRemoveOperation(rootDeploymentAddress2, otherServerGroupDeploymentAddress2));
            }

            ModelNode undeploySg = getEmptyOperation(REMOVE, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS);
            executeOnMaster(undeploySg);

            ModelNode deploySg = getEmptyOperation(ADD, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS);
            deploySg.get(ENABLED).set(true);
            executeOnMaster(deploySg);

        } finally {
            executeOnMaster(localMethods.createRemoveOperation(ROOT_DEPLOYMENT_ADDRESS, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS));
        }
    }

//    @Test
//    public void testRollToServerGroup() throws Exception {
//        // TODO
//        fail("unimplemented");
//    }
//
//    @Test
//    public void testRollToServers() throws Exception {
//        // TODO
//        fail("unimplemented");
//    }
//
//    private void rolloutPlanTest(final ModelNode rolloutPlan) throws Exception {
//        ModelNode content = new ModelNode();
//        content.get(INPUT_STREAM_INDEX).set(0);
//        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, OTHER_SERVER_GROUP_DEPLOYMENT_ADDRESS);
//
//        ModelNode plan = composite.get(OPERATION_HEADERS, ROLLOUT_PLAN).set(rolloutPlan);
//
//        OperationBuilder builder = OperationBuilder.Factory.create(composite);
//        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());
//
//        executeOnMaster(builder.build());
//
//        performHttpCall(DomainTestSupport.masterAddress, 8080);
//        performHttpCall(DomainTestSupport.slaveAddress, 8630);
//
//    }

    private static ModelNode executeOnMaster(ModelNode op) throws IOException {
        return validateResponse(testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op));
    }

    private static ModelNode executeOnMaster(Operation op) throws IOException {
        return validateResponse(testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op));
    }

    private static ModelNode createDeploymentOperation(ModelNode content, ModelNode... serverGroupAddressses) {
        ModelNode composite = getEmptyOperation(COMPOSITE, ROOT_ADDRESS);
        ModelNode steps = composite.get(STEPS);
        ModelNode step1 = steps.add();
        step1.set(getEmptyOperation(ADD, ROOT_DEPLOYMENT_ADDRESS));
        step1.get(CONTENT).add(content);
        for (ModelNode serverGroup : serverGroupAddressses) {
            ModelNode sg = steps.add();
            sg.set(getEmptyOperation(ADD, serverGroup));
            sg.get(ENABLED).set(true);
        }

        return composite;
    }


    private static ModelNode createDeploymentReplaceOperation(ModelNode content, ModelNode... serverGroupAddressses) {
        ModelNode composite = getEmptyOperation(COMPOSITE, ROOT_ADDRESS);
        ModelNode steps = composite.get(STEPS);
        ModelNode step1 = steps.add();
        step1.set(getEmptyOperation(ADD, ROOT_REPLACEMENT_ADDRESS));
        step1.get(RUNTIME_NAME).set(TEST);
        step1.get(CONTENT).add(content);
        for (ModelNode serverGroup : serverGroupAddressses) {
            ModelNode sgr = steps.add();
            sgr.set(getEmptyOperation(REPLACE_DEPLOYMENT, serverGroup));
            sgr.get(ENABLED).set(true);
            sgr.get(NAME).set(REPLACEMENT);
            sgr.get(TO_REPLACE).set(TEST);
        }

        return composite;
    }

    private static ModelNode createDeploymentFullReplaceOperation(ModelNode content) {
        ModelNode op = getEmptyOperation(FULL_REPLACE_DEPLOYMENT, ROOT_ADDRESS);
        op.get(NAME).set(TEST);
        op.get(CONTENT).add(content);
        return op;
    }

    private static List<ModelNode> getDeploymentList(ModelNode address) throws IOException {
        ModelNode op = getEmptyOperation("read-children-names", address);
        op.get("child-type").set("deployment");

        ModelNode response = testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op);
        ModelNode result = validateResponse(response);
        return result.isDefined() ? result.asList() : Collections.<ModelNode>emptyList();
    }

    private static void removeDeployment(String deploymentName, ModelNode address) throws IOException {
        ModelNode deplAddr = new ModelNode();
        deplAddr.set(address);
        deplAddr.add("deployment", deploymentName);
        ModelNode op = getEmptyOperation(REMOVE, deplAddr);
        ModelNode response = testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op);
        validateResponse(response);
    }

    private static ModelNode getEmptyOperation(String operationName, ModelNode address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        if (address != null) {
            op.get(OP_ADDR).set(address);
        }
        else {
            // Just establish the standard structure; caller can fill in address later
            op.get(OP_ADDR);
        }
        return op;
    }

    static byte[] getHash(final ModelNode address) throws IOException {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(CONTENT);

        return executeOnMaster(operation).get(0).get("hash").asBytes();
    }

    private static void performHttpCall(String host, int port) throws IOException {
        performHttpCall(host, port, "test");
    }

    private static void performHttpCall(String host, int port, String context) throws IOException {
        URLConnection conn = null;
        InputStream in = null;
        StringWriter writer = new StringWriter();
        try {
            URL url = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(host) + ":" + port + "/" + context + "/index.html");
            conn = url.openConnection();
            conn.setDoInput(true);
            in = new BufferedInputStream(conn.getInputStream());
            int i = in.read();
            while (i != -1) {
                writer.write((char) i);
                i = in.read();
            }
            assertTrue(writer.toString().indexOf("Hello World") > -1);
        } finally {
            safeClose(in);
            safeClose(writer);
        }
    }

    static void testRemovedContent(final DomainLifecycleUtil util, final byte[] hash) {

        final File home = new File(util.getConfiguration().getDomainDirectory());
        // Domain contents
        final File data = new File(home, "data");
        final File contents = new File(data, "content");
        checkRemoved(contents, hash);

    }

    static void checkRemoved(final File root, final byte[] hash) {

        final String sha1 = HashUtil.bytesToHexString(hash);
        final String partA = sha1.substring(0,2);
        final String partB = sha1.substring(2);

        final File da = new File(root, partA);
        final File db = new File(da, partB);
        final File content = new File(db, "content");

        Assert.assertFalse(content.getAbsolutePath(), content.exists());
        Assert.assertFalse(db.getAbsolutePath(), db.exists());
        if (da.exists()) {
            String[] children = da.list();
            Assert.assertTrue(da.getAbsolutePath(), children != null && children.length > 0);
        }
    }

}
