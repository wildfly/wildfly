package org.jboss.as.test.integration.management.deploy.runtime;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.subsystem.deployment.EJBComponentType;
import org.jboss.as.test.integration.management.deploy.runtime.ejb.statefull.PointLessMathBean;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class StatefulEJBRuntimeNameTestCase extends AbstractRuntimeTestCase {

    private static final String EJB_TYPE = EJBComponentType.STATEFUL.getResourceType();
    private static final Package BEAN_PACKAGE = PointLessMathBean.class.getPackage();
    private static final String BEAN_NAME = "POINT";

    private static final String RT_NAME = "nooma-nooma2-"+EJB_TYPE+".ear";
    private static final String DEPLOYMENT_NAME = "test2-"+EJB_TYPE+"-test.ear";
    private static final String SUB_DEPLOYMENT_NAME = "ejb.jar";
    private static ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();

    @BeforeClass
    public static void setup() throws Exception {

        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, SUB_DEPLOYMENT_NAME);
        ejbJar.addPackage(BEAN_PACKAGE);

        EnterpriseArchive earArchive = ShrinkWrap.create(EnterpriseArchive.class, DEPLOYMENT_NAME);
        earArchive.addAsModule(ejbJar);

        ModelNode addDeploymentOp = new ModelNode();
        addDeploymentOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        addDeploymentOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        addDeploymentOp.get(ModelDescriptionConstants.CONTENT).get(0).get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);
        addDeploymentOp.get(ModelDescriptionConstants.RUNTIME_NAME).set(RT_NAME);
        addDeploymentOp.get(ModelDescriptionConstants.AUTO_START).set(true);
        ModelNode deployOp = new ModelNode();
        deployOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DEPLOY);
        deployOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        deployOp.get(ModelDescriptionConstants.ENABLED).set(true);
        ModelNode[] steps = new ModelNode[2];
        steps[0] = addDeploymentOp;
        steps[1] = deployOp;
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);
        ob.addInputStream(earArchive.as(ZipExporter.class).exportAsInputStream());

        ModelNode result = controllerClient.execute(ob.build());

        // just to blow up
        Assert.assertTrue("Failed to deploy: " + result, Operations.isSuccessfulOutcome(result));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ModelNode result = controllerClient.execute(composite(
                undeploy(DEPLOYMENT_NAME),
                remove(DEPLOYMENT_NAME)
        ));
        // just to blow up
        Assert.assertTrue("Failed to undeploy: " + result, Operations.isSuccessfulOutcome(result));
    }

    @Test
    public void testStepByStep() throws Exception {

        ModelNode readResource = new ModelNode();
        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        readResource.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        ModelNode result = controllerClient.execute(readResource);

        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SUBDEPLOYMENT, SUB_DEPLOYMENT_NAME);
        result = controllerClient.execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SUBSYSTEM, "ejb3");
        result = controllerClient.execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(EJB_TYPE, BEAN_NAME);
        result = controllerClient.execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));
    }

    @Test
    public void testRecursive() throws Exception {

        ModelNode readResource = new ModelNode();
        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        readResource.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        readResource.get(ModelDescriptionConstants.RECURSIVE).set(true);
        ModelNode result = controllerClient.execute(readResource);

        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));
    }

    public StatefulEJBRuntimeNameTestCase() {
        // TODO Auto-generated constructor stub
    }
}
