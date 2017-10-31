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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.http.impl.client.HttpClients;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jboss.as.cli.Util;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.cleanFile;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateResponse;
import static org.junit.Assert.assertEquals;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;

/**
 * Test of various management operations involving deployment overlays
 */
public class DeploymentOverlayTestCase {

    public static final String TEST_OVERLAY = "test";
    public static final String TEST_WILDCARD = "test-server";

    private static final String TEST = "test.war";
    private static final String REPLACEMENT = "test.war.v2";
    private static final ModelNode ROOT_ADDRESS = new ModelNode();
    private static final ModelNode ROOT_DEPLOYMENT_ADDRESS = new ModelNode();
    private static final ModelNode ROOT_REPLACEMENT_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_SERVER_GROUP_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_RUNNING_SERVER_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_RUNNING_SERVER_DEPLOYMENT_ADDRESS = new ModelNode();
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
        MAIN_RUNNING_SERVER_ADDRESS.add(HOST, "master");
        MAIN_RUNNING_SERVER_ADDRESS.add(SERVER, "main-one");
        MAIN_RUNNING_SERVER_ADDRESS.protect();
        MAIN_RUNNING_SERVER_DEPLOYMENT_ADDRESS.add(HOST, "master");
        MAIN_RUNNING_SERVER_DEPLOYMENT_ADDRESS.add(SERVER, "main-one");
        MAIN_RUNNING_SERVER_DEPLOYMENT_ADDRESS.add(DEPLOYMENT, TEST);
        MAIN_RUNNING_SERVER_DEPLOYMENT_ADDRESS.protect();

    }

    private static DomainTestSupport testSupport;
    private static WebArchive webArchive;
    private static File tmpDir;

    @BeforeClass
    public static void setupDomain() throws Exception {

        // Create our deployment
        webArchive = ShrinkWrap.create(WebArchive.class, TEST);
        webArchive.addAsWebInfResource("deploymentoverlay/web.xml", "web.xml");
        webArchive.addClass(DeploymentOverlayServlet.class);


        // Make versions on the filesystem for URL-based deploy and for unmanaged content testing
        tmpDir = new File("target/deployments/" + DeploymentOverlayTestCase.class.getSimpleName());
        new File(tmpDir, "archives").mkdirs();
        new File(tmpDir, "exploded").mkdirs();
        webArchive.as(ZipExporter.class).exportTo(new File(tmpDir, "archives/" + TEST), true);
        webArchive.as(ExplodedExporter.class).exportExploded(new File(tmpDir, "exploded"));

        // Launch the domain
        testSupport = DomainTestSuite.createSupport(DeploymentOverlayTestCase.class.getSimpleName());


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
    }

    /**
     * Remove all deployments from the model.
     *
     * @throws java.io.IOException
     */
    private void cleanDeployments() throws IOException {
        List<ModelNode> deploymentList = getDeploymentList(MAIN_SERVER_GROUP_ADDRESS);
        for (ModelNode deployment : deploymentList) {
            removeDeployment(deployment.asString(), MAIN_SERVER_GROUP_ADDRESS);
        }
        deploymentList = getDeploymentList(ROOT_ADDRESS);
        for (ModelNode deployment : deploymentList) {
            removeDeployment(deployment.asString(), ROOT_ADDRESS);
        }
    }

    public void setupDeploymentOverride() throws Exception {

        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        executeOnMaster(op);


        //add an override that will not be linked via a wildcard
        //add the content
        op = new ModelNode();
        OperationBuilder builder = new OperationBuilder(op, true);
        ModelNode addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
        addr.add(ModelDescriptionConstants.CONTENT, "WEB-INF/web.xml");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        op.get(ModelDescriptionConstants.CONTENT).get(INPUT_STREAM_INDEX).set(0);
        builder.addInputStream(getClass().getClassLoader().getResourceAsStream("deploymentoverlay/override.xml"));
        executeOnMaster(builder.build());

        //add the non-wildcard link to the server group
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.SERVER_GROUP, "main-server-group");
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        executeOnMaster(op);

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.SERVER_GROUP, "main-server-group");
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
        addr.add(ModelDescriptionConstants.DEPLOYMENT, "test.war");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        executeOnMaster(op);


        //add the wildard link
        final ModelNode composite = new ModelNode();
        final OperationBuilder opBuilder = new OperationBuilder(composite, true);
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);


        op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        steps.add(op);

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
        addr.add(ModelDescriptionConstants.CONTENT, "WEB-INF/web.xml");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.BYTES).set(FileUtils.readFile(getClass().getClassLoader().getResource("deploymentoverlay/wildcard-override.xml")).getBytes());

        steps.add(op);

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
        addr.add(ModelDescriptionConstants.CONTENT, "wildcard-new-file.txt");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);
        opBuilder.addInputStream(new ByteArrayInputStream("new file".getBytes()));
        steps.add(op);

        //add the non-wildcard link to the server group
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.SERVER_GROUP, "main-server-group");
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        steps.add(op);

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.SERVER_GROUP, "main-server-group");
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
        addr.add(ModelDescriptionConstants.DEPLOYMENT, "*.war");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        steps.add(op);

        executeOnMaster(opBuilder.build());
    }


    /**
     * This test creates and links two deployment overlays, does a deployment, and then tests that the overlay has taken effect
     * @throws Exception
     */
    @Test
    public void testDeploymentOverlayInDomainMode() throws Exception {

        setupDeploymentOverride();

        ModelNode content = new ModelNode();
        content.get(INPUT_STREAM_INDEX).set(0);
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        OperationBuilder builder = new OperationBuilder(composite, true);
        builder.addInputStream(webArchive.as(ZipExporter.class).exportAsInputStream());
        executeOnMaster(builder.build());

        DomainClient client = testSupport.getDomainMasterLifecycleUtil().createDomainClient();
        Assert.assertEquals("OVERRIDDEN", performHttpCall(client, "master", "main-one", "standard-sockets", "/test/servlet"));
        Assert.assertEquals("OVERRIDDEN", performHttpCall(client, "slave", "main-three", "standard-sockets", "/test/servlet"));
        Assert.assertEquals("new file", performHttpCall(client, "master", "main-one", "standard-sockets", "/test/wildcard-new-file.txt"));
        Assert.assertEquals("new file", performHttpCall(client, "slave", "main-three", "standard-sockets", "/test/wildcard-new-file.txt"));

        //Remove the wildcard overlay
        ModelNode op = Operations.createRemoveOperation(PathAddress.pathAddress(ModelDescriptionConstants.SERVER_GROUP, "main-server-group")
                .append(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD)
                .append(ModelDescriptionConstants.DEPLOYMENT, "*.war")
                .toModelNode());
        op.get("redeploy-affected").set(true);
        executeOnMaster(op);
        Assert.assertEquals("OVERRIDDEN", performHttpCall(client, "master", "main-one", "standard-sockets", "/test/servlet"));
        Assert.assertEquals("OVERRIDDEN", performHttpCall(client, "slave", "main-three", "standard-sockets", "/test/servlet"));
        Assert.assertEquals("<html><head><title>Error</title></head><body>Not Found</body></html>", performHttpCall(client, "master", "main-one", "standard-sockets", "/test/wildcard-new-file.txt"));
        Assert.assertEquals("<html><head><title>Error</title></head><body>Not Found</body></html>", performHttpCall(client, "slave", "main-three", "standard-sockets", "/test/wildcard-new-file.txt"));
        op = Operations.createRemoveOperation(PathAddress.pathAddress(ModelDescriptionConstants.SERVER_GROUP, "main-server-group")
                .append(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY)
                .append(ModelDescriptionConstants.DEPLOYMENT, "test.war")
                .toModelNode());
        op.get("redeploy-affected").set(true);
        executeOnMaster(op);
        Assert.assertEquals("NON OVERRIDDEN", performHttpCall(client, "master", "main-one", "standard-sockets", "/test/servlet"));
        Assert.assertEquals("NON OVERRIDDEN", performHttpCall(client, "slave", "main-three", "standard-sockets", "/test/servlet"));
        Assert.assertEquals("<html><head><title>Error</title></head><body>Not Found</body></html>", performHttpCall(client, "master", "main-one", "standard-sockets", "/test/wildcard-new-file.txt"));
        Assert.assertEquals("<html><head><title>Error</title></head><body>Not Found</body></html>", performHttpCall(client, "slave", "main-three", "standard-sockets", "/test/wildcard-new-file.txt"));
    }

    private String performHttpCall(DomainClient client, String host, String server, String socketBindingGroup, String path) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).add(HOST, host).add(SERVER, server).add(SOCKET_BINDING_GROUP, socketBindingGroup).add(SOCKET_BINDING, "http");
        op.get(INCLUDE_RUNTIME).set(true);
        ModelNode socketBinding = validateResponse(client.execute(op));

        URL url = new URL("http",
                TestSuiteEnvironment.formatPossibleIpv6Address(socketBinding.get("bound-address").asString()),
                socketBinding.get("bound-port").asInt(),
                path);
        HttpGet get = new HttpGet(url.toURI());
        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(get);
        return getContent(response);
    }

    public static String getContent(HttpResponse response) throws IOException {
        InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[8];
        int c;
        while ((c = reader.read(buffer)) != -1) {
            content.append(buffer, 0, c);
        }
        reader.close();
        return content.toString();
    }

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

}
