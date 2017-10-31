package org.jboss.as.test.integration.deployment.deploymentoverlay;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@ServerSetup(DeploymentOverlayTestCase.DeploymentOverlayTestCaseServerSetup.class)
public class DeploymentOverlayTestCase {

    public static final String TEST_OVERLAY = "test";
    public static final String TEST_WILDCARD = "test-wildcard";

    static class DeploymentOverlayTestCaseServerSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

            ModelNode op = new ModelNode();
            op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);


            //add an override that will not be linked via a wildcard
            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP_ADDR).set(new ModelNode());
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UPLOAD_DEPLOYMENT_BYTES);
            op.get(ModelDescriptionConstants.BYTES).set(FileUtils.readFile(DeploymentOverlayTestCase.class, "override.xml").getBytes());
            ModelNode result = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            //add the content
            op = new ModelNode();
            ModelNode addr = new ModelNode();
            addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
            addr.add(ModelDescriptionConstants.CONTENT, "WEB-INF/web.xml");
            op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.HASH).set(result);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            //add the non-wildcard link
            op = new ModelNode();
            addr = new ModelNode();
            addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_OVERLAY);
            addr.add(ModelDescriptionConstants.DEPLOYMENT, "test.war");
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
            op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.BYTES).set(FileUtils.readFile(DeploymentOverlayTestCase.class, "wildcard-override.xml").getBytes());
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            op = new ModelNode();
            addr = new ModelNode();
            addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
            addr.add(ModelDescriptionConstants.CONTENT, "WEB-INF/classes/wildcard-new-file");
            op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);


            OperationBuilder builder = new OperationBuilder(op, true);
            builder.addInputStream(DeploymentOverlayTestCase.class.getResourceAsStream("wildcard-new-file"));
            ManagementOperations.executeOperation(managementClient.getControllerClient(), builder.build());

            //add the wildcard link
            op = new ModelNode();
            addr = new ModelNode();
            addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, TEST_WILDCARD);
            addr.add(ModelDescriptionConstants.DEPLOYMENT, "*.war");
            op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {

            removeContentItem(managementClient, TEST_OVERLAY, "WEB-INF/web.xml");
            removeDeploymentItem(managementClient, TEST_OVERLAY, "test.war");

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

        private void removeContentItem(final ManagementClient managementClient, final String overlayName, final String content) throws IOException, MgmtOperationException {
            final ModelNode addr = new ModelNode();
            addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, overlayName);
            addr.add(ModelDescriptionConstants.CONTENT, content);
            final ModelNode op = Operations.createRemoveOperation(addr);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }


        private void removeDeploymentItem(final ManagementClient managementClient, final String overlayName, final String deploymentRuntimeName) throws IOException, MgmtOperationException {
            final ModelNode addr = new ModelNode();
            addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, overlayName);
            addr.add(ModelDescriptionConstants.DEPLOYMENT, deploymentRuntimeName);
            final ModelNode op = Operations.createRemoveOperation(addr);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }
    }

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addPackage(DeploymentOverlayTestCase.class.getPackage())
                .setWebXML(DeploymentOverlayTestCase.class.getPackage(), "web.xml");
    }

    @ArquillianResource
    private InitialContext initialContext;

    @Test
    public void testContentOverridden() throws NamingException {
        Assert.assertEquals("OVERRIDDEN", initialContext.lookup("java:module/env/simpleString"));

    }

    @Test
    public void testAddingNewFile() {
        Assert.assertNotNull(getClass().getClassLoader().getResource("wildcard-new-file"));
    }

}
