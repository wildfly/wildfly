/*
 * Copyright (C) 2016 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.test.integration.deployment.deploymentoverlay;


import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;


import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a>  (c) 2015 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ExplodedDeploymentOverlayTestCase {

    public static final String TEST_OVERLAY = "test";
    public static final String TEST_WILDCARD = "test-wildcard";

    @ArquillianResource
    private ManagementClient managementClient;

    @Before
    public void setup() throws Exception {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        //add an override that will not be linked via a wildcard
        //add the content
        op = new ModelNode();
        ModelNode addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
        addr.add(ModelDescriptionConstants.CONTENT, "WEB-INF/web.xml");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);

        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(ExplodedDeploymentOverlayTestCase.class.getResourceAsStream("override.xml"));
        ManagementOperations.executeOperation(managementClient.getControllerClient(), builder.build());

        //add the non-wildcard link
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
        addr.add(ModelDescriptionConstants.DEPLOYMENT, "exploded-test.war");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        //add the deployment overlay that will be linked via wildcard
        op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
        addr.add(ModelDescriptionConstants.CONTENT, "WEB-INF/web.xml");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.BYTES).set(
                FileUtils.readFile(ExplodedDeploymentOverlayTestCase.class, "wildcard-override.xml").getBytes());
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
        addr.add(ModelDescriptionConstants.CONTENT, "WEB-INF/classes/wildcard-new-file");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);

        builder = new OperationBuilder(op, true);
        builder.addInputStream(ExplodedDeploymentOverlayTestCase.class.getResourceAsStream("wildcard-new-file"));
        ManagementOperations.executeOperation(managementClient.getControllerClient(), builder.build());

        //add the wildcard link
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
        addr.add(ModelDescriptionConstants.DEPLOYMENT, "*.war");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        //Deploy exploded deployement
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT, "exploded-test.war");

        op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        op.get(ModelDescriptionConstants.NAME).set("content[0]");
        ModelNode result = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        if (!result.hasDefined(ModelDescriptionConstants.ARCHIVE) || result.get(ModelDescriptionConstants.ARCHIVE).asBoolean(true)) {
            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UNDEPLOY);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP).set("explode");
            op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DEPLOY);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }
    }

    @After
    public void tearDown() throws Exception {

        removeContentItem(managementClient, TEST_OVERLAY, "WEB-INF/web.xml");
        removeDeploymentItem(managementClient, TEST_OVERLAY, "exploded-test.war");

        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        removeDeploymentItem(managementClient, TEST_WILDCARD, "*.war");
        removeContentItem(managementClient, TEST_WILDCARD, "WEB-INF/web.xml");

        removeContentItem(managementClient, TEST_WILDCARD, "WEB-INF/classes/wildcard-new-file");

        op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }

    private void removeContentItem(final ManagementClient managementClient, final String w, final String a) throws IOException, MgmtOperationException {
        final ModelNode op;
        final ModelNode addr;
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, w);
        addr.add(ModelDescriptionConstants.CONTENT, a);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }

    private void removeDeploymentItem(final ManagementClient managementClient, final String w, final String a) throws IOException, MgmtOperationException {
        final ModelNode op;
        final ModelNode addr;
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, w);
        addr.add(ModelDescriptionConstants.DEPLOYMENT, a);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }

    private void updateDeployment(String file, String targetPath) throws IOException, MgmtOperationException {
        final ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set("add-content");
        final ModelNode addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT, "exploded-test.war");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        ModelNode updatedFile = new ModelNode();
        updatedFile.get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);
        updatedFile.get("target-path").set(targetPath);
        op.get(ModelDescriptionConstants.CONTENT).add(updatedFile);
        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(ExplodedDeploymentOverlayTestCase.class.getResourceAsStream(file));
        ManagementOperations.executeOperation(managementClient.getControllerClient(), builder.build());
    }

    @Test
    public void testContentOverridden() throws Exception {
        final String requestURL = managementClient.getWebUri() + "/exploded-test/simple/";
        final HttpGet request = new HttpGet(requestURL);
        final HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(request);
        String responseMessage = EntityUtils.toString(response.getEntity());
        Assert.assertEquals("OVERRIDDEN",responseMessage);
        updateDeployment("update.xml","WEB-INF/web.xml");
        response = httpClient.execute(request);
        responseMessage = EntityUtils.toString(response.getEntity());
        Assert.assertEquals("OVERRIDDEN",responseMessage);
        updateDeployment("web.xml","WEB-INF/web.xml");
    }

    @Test
    public void testAddingNewFile() throws Exception {
        final HttpClient httpClient = new DefaultHttpClient();
        String requestURL = managementClient.getWebUri() + "/exploded-test/overlay/";
        HttpGet request = new HttpGet(requestURL);
        HttpResponse response = httpClient.execute(request);
        String responseMessage = EntityUtils.toString(response.getEntity());
        Assert.assertEquals("test",responseMessage);
        updateDeployment("web.xml", "WEB-INF/classes/wildcard-new-file");
        response = httpClient.execute(request);
        responseMessage = EntityUtils.toString(response.getEntity());
        Assert.assertEquals("test",responseMessage);
        updateDeployment("web.xml", "WEB-INF/classes/wildcard-new-file");
        updateDeployment("index.html", "index.html");
        requestURL = managementClient.getWebUri() + "/exploded-test/index.html";
        request = new HttpGet(requestURL);
        response = httpClient.execute(request);
        responseMessage = EntityUtils.toString(response.getEntity());
        Assert.assertTrue(responseMessage, responseMessage.contains("Simple Content test for exploded-test.war"));
    }

    @Deployment()
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "exploded-test.war")
                .addClass(SimpleServlet.class)
                .addClass(OverlayServlet.class)
                .setWebXML(SimpleServlet.class.getPackage(), "web.xml");
    }
}
