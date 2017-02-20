/*
 * Copyright 2017 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.deployment.deploymentoverlay;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
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
 * @author Stuart Douglas
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AffectedDeploymentOverlayTestCase extends ContainerResourceMgmtTestBase {

    private static final String DEPLOYMENT_NAME = "test.war";
    public static final String TEST_OVERLAY = "test";
    public static final String TEST_WILDCARD = "test-wildcard";
    private static final PathAddress TEST_OVERLAY_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
    private static final PathAddress TEST_WILDCARD_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);

    @Before
    public void setup() throws Exception {
        getModelControllerClient().execute(Operations.createAddOperation(TEST_OVERLAY_ADDRESS.toModelNode()));

        //add an override that will not be linked via a wildcard
        //add the content
        ModelNode op = Operations.createAddOperation(TEST_OVERLAY_ADDRESS.append(ModelDescriptionConstants.CONTENT, "WEB-INF/web.xml").toModelNode());
        op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);
        OperationBuilder builder = new OperationBuilder(op, true);
        builder.addInputStream(AffectedDeploymentOverlayTestCase.class.getResourceAsStream("override.xml"));
        getModelControllerClient().execute(builder.build());

        //add the non-wildcard link
        getModelControllerClient().execute(Operations.createAddOperation(TEST_OVERLAY_ADDRESS.append(DEPLOYMENT, DEPLOYMENT_NAME).toModelNode()));

        //add the deployment overlay that will be linked via wildcard
        getModelControllerClient().execute(Operations.createAddOperation(TEST_WILDCARD_ADDRESS.toModelNode()));

        op = Operations.createAddOperation(TEST_WILDCARD_ADDRESS.append(ModelDescriptionConstants.CONTENT, "WEB-INF/web.xml").toModelNode());
        op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.BYTES).set(FileUtils.readFile(AffectedDeploymentOverlayTestCase.class, "wildcard-override.xml").getBytes());
        getModelControllerClient().execute(op);

        op = Operations.createAddOperation(TEST_WILDCARD_ADDRESS.append(ModelDescriptionConstants.CONTENT, "WEB-INF/classes/wildcard-new-file").toModelNode());
        op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);

        builder = new OperationBuilder(op, true);
        builder.addInputStream(AffectedDeploymentOverlayTestCase.class.getResourceAsStream("wildcard-new-file"));
        getModelControllerClient().execute(builder.build());
    }

    @After
    public void tearDown() throws Exception {
        getModelControllerClient().execute(Operations.createRemoveOperation(TEST_OVERLAY_ADDRESS.append(CONTENT, "WEB-INF/web.xml").toModelNode()));
        getModelControllerClient().execute(Operations.createRemoveOperation(TEST_OVERLAY_ADDRESS.append(DEPLOYMENT, DEPLOYMENT_NAME).toModelNode()));
        getModelControllerClient().execute(Operations.createRemoveOperation(TEST_OVERLAY_ADDRESS.toModelNode()));

        getModelControllerClient().execute(Operations.createRemoveOperation(TEST_WILDCARD_ADDRESS.append(DEPLOYMENT, "*.war").toModelNode()));
        getModelControllerClient().execute(Operations.createRemoveOperation(TEST_WILDCARD_ADDRESS.append(CONTENT, "WEB-INF/web.xml").toModelNode()));
        getModelControllerClient().execute(Operations.createRemoveOperation(TEST_WILDCARD_ADDRESS.append(CONTENT, "WEB-INF/classes/wildcard-new-file").toModelNode()));
        getModelControllerClient().execute(Operations.createRemoveOperation(TEST_WILDCARD_ADDRESS.toModelNode()));
    }

    @Deployment(name = DEPLOYMENT_NAME)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addPackage(AffectedDeploymentOverlayTestCase.class.getPackage())
                .setWebXML(AffectedDeploymentOverlayTestCase.class.getPackage(), "web.xml");
    }

    @Test
    public void testContentOverridden(@ArquillianResource URL url) throws Exception {
        String response = HttpRequest.get(url + "/simple/", 10, TimeUnit.SECONDS);
        Assert.assertEquals("Overlay doesn't contain valid output", "UPDATED", response.trim());
        try {
            HttpRequest.get(url + "/overlay/", 10, TimeUnit.SECONDS);
            Assert.fail("Overlay servlet shouldn't be up and working properly");
        } catch (IOException ioex) {
            Assert.assertThat(ioex.getMessage(), CoreMatchers.containsString("HTTP Status 500 Response:"));
        }
        getModelControllerClient().execute(Operations.createOperation("redeploy-links", TEST_OVERLAY_ADDRESS.toModelNode()));
        response = HttpRequest.get(url + "/simple/", 10, TimeUnit.SECONDS);
        Assert.assertEquals("Overlay doesn't contain valid output", "OVERRIDDEN", response.trim());
        try {
            HttpRequest.get(url + "/overlay/", 10, TimeUnit.SECONDS);
            Assert.fail("Overlay servlet shouldn't be up and working properly");
        } catch (IOException ioex) {
            Assert.assertThat(ioex.getMessage(), CoreMatchers.containsString("HTTP Status 500 Response:"));
        }
        //add the wildcard link
        getModelControllerClient().execute(Operations.createAddOperation(TEST_WILDCARD_ADDRESS.append(ModelDescriptionConstants.DEPLOYMENT, "*.war").toModelNode()));
        getModelControllerClient().execute(Operations.createOperation("redeploy-links", TEST_OVERLAY_ADDRESS.toModelNode()));//This will redeploy the deployment
        response = HttpRequest.get(url + "/simple/", 10, TimeUnit.SECONDS);
        Assert.assertEquals("Overlay doesn't contain valid output", "OVERRIDDEN", response.trim());
        response = HttpRequest.get(url + "/overlay/", 10, TimeUnit.SECONDS);
        Assert.assertEquals("Overlay doesn't contain valid output", "test", response.trim());
        getModelControllerClient().execute(Operations.createOperation("redeploy-links", TEST_WILDCARD_ADDRESS.toModelNode()));//This will redeploy the deployment
        response = HttpRequest.get(url + "/simple/", 10, TimeUnit.SECONDS);
        Assert.assertEquals("Overlaydoesn't contain valid output", "OVERRIDDEN", response.trim());
        response = HttpRequest.get(url + "/overlay/", 10, TimeUnit.SECONDS);
        Assert.assertEquals("Overlay doesn't contain valid output", "test", response.trim());
    }

}
