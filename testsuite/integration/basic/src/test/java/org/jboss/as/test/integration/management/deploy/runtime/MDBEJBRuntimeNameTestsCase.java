package org.jboss.as.test.integration.management.deploy.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.subsystem.deployment.EJBComponentType;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.deploy.runtime.ejb.message.Constants;
import org.jboss.as.test.integration.management.deploy.runtime.ejb.message.SimpleMDB;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class MDBEJBRuntimeNameTestsCase extends AbstractRuntimeTestCase {

    private static final Logger log = Logger.getLogger(MDBEJBRuntimeNameTestsCase.class);

    private static final String QUEUE_NAME = "Queue-for-" + MDBEJBRuntimeNameTestsCase.class.getName();

    private static final String EJB_TYPE = EJBComponentType.MESSAGE_DRIVEN.getResourceType();
    private static final Class BEAN_CLASS = SimpleMDB.class;
    private static final Package BEAN_PACKAGE = BEAN_CLASS.getPackage();
    private static final String BEAN_NAME = "POINT";

    private static final String RT_MODULE_NAME = "nooma-nooma5-" + EJB_TYPE;
    private static final String RT_NAME = RT_MODULE_NAME + ".ear";
    private static final String DEPLOYMENT_MODULE_NAME = "test5-" + EJB_TYPE + "-test";
    private static final String DEPLOYMENT_NAME = DEPLOYMENT_MODULE_NAME + ".ear";
    private static final String SUB_DEPLOYMENT_MODULE_NAME = "ejb";
    private static final String SUB_DEPLOYMENT_NAME = SUB_DEPLOYMENT_MODULE_NAME + ".jar";

    @ContainerResource
    private InitialContext context;
    @ContainerResource
    private ManagementClient managementClient;

    private JMSOperations adminSupport;

    @Before
    public void setup() throws Exception {
        adminSupport = JMSOperationsProvider.getInstance(managementClient);
        //Remote JMS - bind name... to make it available remotely, lookup original name.
        adminSupport.createJmsQueue(QUEUE_NAME, "java:jboss/exported/" + Constants.QUEUE_JNDI_NAME);
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, SUB_DEPLOYMENT_NAME);
        ejbJar.addPackage(BEAN_PACKAGE);
        ejbJar.setManifest(new StringAsset(
                Descriptors.create(ManifestDescriptor.class)
                        .attribute("Dependencies", "org.apache.activemq.artemis.ra")
                        .exportAsString()));
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

        ModelNode result = managementClient.getControllerClient().execute(ob.build());

        // just to blow up
        Assert.assertTrue("Failed to deploy: " + result, Operations.isSuccessfulOutcome(result));

    }

    @After
    public void tearDown() throws Exception {
        ModelNode result = managementClient.getControllerClient().execute(composite(
                undeploy(DEPLOYMENT_NAME),
                remove(DEPLOYMENT_NAME)
        ));
        // just to blow up
        Assert.assertTrue("Failed to undeploy: " + result, Operations.isSuccessfulOutcome(result));
        adminSupport.removeJmsQueue(QUEUE_NAME);
    }

    @Test
    @InSequence(1)
    public void testMDB() throws Exception {

        final QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup("java:/jms/RemoteConnectionFactory");

        final QueueConnection connection = factory.createQueueConnection("guest", "guest");
        try {
            connection.start();
            final QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            final Queue replyDestination = session.createTemporaryQueue();
            final String requestMessage = "test";
            final Message message = session.createTextMessage(requestMessage);
            message.setJMSReplyTo(replyDestination);
            final Destination destination = (Destination) context.lookup(Constants.QUEUE_JNDI_NAME);
            final MessageProducer producer = session.createProducer(destination);
            producer.send(message);
            producer.close();

            // wait for a reply
            final QueueReceiver receiver = session.createReceiver(replyDestination);
            final Message reply = receiver.receive(TimeoutUtil.adjust(1000));
            assertNotNull(
                    "Did not receive a reply on the reply queue. Perhaps the original (request) message didn't make it to the MDB?",
                    reply);
            final String result = ((TextMessage) reply).getText();
            assertEquals("Unexpected reply messsage", Constants.REPLY_MESSAGE_PREFIX + requestMessage, result);
        } finally {
            if (connection != null) {
                // just closing the connection will close the session and other related resources (@see javax.jms.Connection)
                safeClose(connection);
            }
        }
    }

    @Test
    @InSequence(2)
    public void testStepByStep() throws Exception {

        ModelNode readResource = new ModelNode();
        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        readResource.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        ModelNode result = managementClient.getControllerClient().execute(readResource);

        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SUBDEPLOYMENT, SUB_DEPLOYMENT_NAME);
        result = managementClient.getControllerClient().execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SUBSYSTEM, "ejb3");
        result = managementClient.getControllerClient().execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(EJB_TYPE, BEAN_NAME);
        result = managementClient.getControllerClient().execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));
    }

    @Test
    @InSequence(3)
    public void testRecursive() throws Exception {

        ModelNode readResource = new ModelNode();
        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        readResource.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        readResource.get(ModelDescriptionConstants.RECURSIVE).set(true);
        ModelNode result = managementClient.getControllerClient().execute(readResource);

        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));
    }

    private static void safeClose(final Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Throwable t) {
            // just log
            log.trace("Ignoring a problem which occurred while closing: " + connection, t);
        }
    }
}
