/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FULL_REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_REPLACE;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.xnio.IoUtils.safeClose;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.mixed.jsf.Bean;
import org.jboss.as.test.integration.domain.mixed.util.MixedDomainTestSupport;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class MixedDomainDeploymentTest {

    private static final String TEST = "test.war";
    private static final String REPLACEMENT = "test.war.v2";
    private static final ModelNode ROOT_ADDRESS = new ModelNode();
    private static final ModelNode ROOT_DEPLOYMENT_ADDRESS = new ModelNode();
    private static final ModelNode ROOT_REPLACEMENT_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_SERVER_GROUP_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS = new ModelNode();

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
    }

    private WebArchive webArchive;
    private WebArchive webArchive2;
    private WebArchive jsfTestArchive;
    private MixedDomainTestSupport testSupport;
    private File tmpDir;



    @Before
    public void setupDomain() throws Exception {

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
        tmpDir = new File("target/deployments/" + this.getClass().getSimpleName());
        new File(tmpDir, "archives").mkdirs();
        new File(tmpDir, "exploded").mkdirs();
        webArchive.as(ZipExporter.class).exportTo(new File(tmpDir, "archives/" + TEST), true);
        webArchive.as(ExplodedExporter.class).exportExploded(new File(tmpDir, "exploded"));

        //Make the jsf war to test that jsf works on the older slaves that did not have the jsf subsystem
        jsfTestArchive = ShrinkWrap.create(WebArchive.class, "jsf-test.war");
        jsfTestArchive.addClass(Bean.class);
        jsfTestArchive.addAsWebResource("jsf-test/index.html");
        jsfTestArchive.addAsWebResource("jsf-test/home.xhtml");
        jsfTestArchive.addAsWebInfResource("jsf-test/WEB-INF/beans.xml");
        jsfTestArchive.addAsWebInfResource("jsf-test/WEB-INF/faces-config.xml");


        // Launch the domain
        testSupport = MixedDomainTestSuite.getSupport(this.getClass());
        confirmNoDeployments();
    }

    @AfterClass
    public synchronized static void afterClass() {
        MixedDomainTestSuite.afterClass();
    }

    /**
     * Validate that there are no deployments; try and clean if there are.
     *
     * @throws Exception
     */
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

    @Test
    public void testDeploymentViaUrl() throws Exception {
        String url = new File(tmpDir, "archives/" + TEST).toURI().toURL().toString();
        ModelNode content = new ModelNode();
        content.get("url").set(url);
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, getOtherServerGroupDeploymentAddress());
        System.out.println(composite);
        executeOnMaster(composite);

        performHttpCall(DomainTestSupport.slaveAddress, 8080);
    }

    @Test
    public void testDeploymentViaStream() throws Exception {
        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, getOtherServerGroupDeploymentAddress());
        OperationBuilder builder = new OperationBuilder(composite, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());

        performHttpCall(DomainTestSupport.slaveAddress, 8080);

    }

    @Test
    public void testUnmanagedArchiveDeployment() throws Exception {
        ModelNode content = new ModelNode();
        content.get("archive").set(true);
        content.get("path").set(new File(tmpDir, "archives/" + TEST).getAbsolutePath());
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, getOtherServerGroupDeploymentAddress());
        executeOnMaster(composite);

        performHttpCall(DomainTestSupport.slaveAddress, 8080);
    }

    @Test
    public void testUnmanagedExplodedDeployment() throws Exception {
        ModelNode content = new ModelNode();
        content.get("archive").set(false);
        content.get("path").set(new File(tmpDir, "exploded/" + TEST).getAbsolutePath());
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, getOtherServerGroupDeploymentAddress());

        executeOnMaster(composite);

        performHttpCall(DomainTestSupport.slaveAddress, 8080);
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

    @Test
    public void testReplace() throws Exception {
        // Establish the deployment
        testDeploymentViaStream();

        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode op = createDeploymentReplaceOperation(content, MAIN_SERVER_GROUP_ADDRESS, getOtherServerGroupAddress());
        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());

        performHttpCall(DomainTestSupport.slaveAddress, 8080);
    }

    @Test
    public void testJsfWorks() throws Exception {
        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        //Just be lazy here and deploy the jsf-test.war with the same name as the other deployments we tried
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS, getOtherServerGroupDeploymentAddress());
        OperationBuilder builder = new OperationBuilder(composite, true);
        builder.addInputStream(jsfTestArchive.as(ZipExporter.class).exportAsInputStream());

        executeOnMaster(builder.build());

        performHttpCall(DomainTestSupport.slaveAddress, 8080, "test/home.jsf", "Bean Works");
    }

    private void redeployTest() throws IOException {
        ModelNode op = getEmptyOperation("redeploy", getOtherServerGroupDeploymentAddress());
        executeOnMaster(op);

        performHttpCall(DomainTestSupport.slaveAddress, 8080);
    }


    private void undeployTest() throws Exception {
        ModelNode op = getEmptyOperation("undeploy", getOtherServerGroupDeploymentAddress());
        executeOnMaster(op);

        // Thread.sleep(1000);

        try {
            performHttpCall(DomainTestSupport.slaveAddress, 8080);
            fail("Webapp still accessible following undeploy");
        } catch (IOException good) {
            // desired result
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
        deploymentList = getDeploymentList(getOtherServerGroupAddress());
        for (ModelNode deployment : deploymentList) {
            removeDeployment(deployment.asString(), getOtherServerGroupAddress());
        }
        deploymentList = getDeploymentList(ROOT_ADDRESS);
        for (ModelNode deployment : deploymentList) {
            removeDeployment(deployment.asString(), ROOT_ADDRESS);
        }
    }

    private ModelNode executeOnMaster(ModelNode op) throws IOException {
        return validateResponse(testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op));
    }

    private ModelNode executeOnMaster(Operation op) throws IOException {
        return validateResponse(testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op));
    }

    private ModelNode createDeploymentOperation(ModelNode content, ModelNode... serverGroupAddressses) {
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


    private ModelNode createDeploymentReplaceOperation(ModelNode content, ModelNode... serverGroupAddressses) {
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

    private ModelNode createDeploymentFullReplaceOperation(ModelNode content) {
        ModelNode op = getEmptyOperation(FULL_REPLACE_DEPLOYMENT, ROOT_ADDRESS);
        op.get(NAME).set(TEST);
        op.get(CONTENT).add(content);
        return op;
    }

    private List<ModelNode> getDeploymentList(ModelNode address) throws IOException {
        ModelNode op = getEmptyOperation("read-children-names", address);
        op.get("child-type").set("deployment");

        ModelNode response = testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op);
        ModelNode result = validateResponse(response);
        return result.isDefined() ? result.asList() : Collections.<ModelNode>emptyList();
    }

    private void performHttpCall(String host, int port) throws IOException {
        performHttpCall(host, port, "test/index.html", "Hello World");
    }

    private void performHttpCall(String host, int port, String path, String expected) throws IOException {
        URLConnection conn = null;
        InputStream in = null;
        StringWriter writer = new StringWriter();
        try {
            URL url = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(host) + ":" + port + "/" + path);
            System.out.println("Reading response from " + url + ":");
            conn = url.openConnection();
            conn.setDoInput(true);
            in = new BufferedInputStream(conn.getInputStream());
            int i = in.read();
            while (i != -1) {
                writer.write((char) i);
                i = in.read();
            }
            assertTrue((writer.toString().indexOf(expected) > -1));
            System.out.println("OK");
        } finally {
            safeClose(in);
            safeClose(writer);
        }
    }

    private void removeDeployment(String deploymentName, ModelNode address) throws IOException {
        ModelNode deplAddr = new ModelNode();
        deplAddr.set(address);
        deplAddr.add("deployment", deploymentName);
        ModelNode op = getEmptyOperation(REMOVE, deplAddr);
        ModelNode response = testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op);
        validateResponse(response);
    }

    private ModelNode getEmptyOperation(String operationName, ModelNode address) {
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

    private ModelNode getOtherServerGroupAddress() {
        ModelNode addr = new ModelNode();
        addr.add(SERVER_GROUP, getServerGroupName());
        return addr;
    }
    private ModelNode getOtherServerGroupDeploymentAddress() {
        ModelNode addr = new ModelNode();
        addr.add(SERVER_GROUP, getServerGroupName());
        addr.add(DEPLOYMENT, TEST);
        return addr;
    }

    abstract String getServerGroupName();

}
