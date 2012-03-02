package org.jboss.as.test.integration.ejb.mdb.resourceadapter;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ConfiguredResourceAdapterNameTestCase {

    private static final Logger logger = Logger.getLogger(ConfiguredResourceAdapterNameTestCase.class);

    private static final String REPLY_QUEUE_JNDI_NAME = "java:jboss/override-resource-adapter-name-test/replyQueue";
    public static final String QUEUE_JNDI_NAME = "java:jboss/override-resource-adapter-name-test/queue";

    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource(mappedName = REPLY_QUEUE_JNDI_NAME)
    private Queue replyQueue;

    @Resource(mappedName = QUEUE_JNDI_NAME)
    private Queue queue;

    private static JMSOperations jmsAdminOperations;

    @Deployment
    public static Archive<JavaArchive> getDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "configured-resource-adapter-name-mdb-test.jar")
                .addClasses(ConfiguredResourceAdapterNameMDB.class, JMSMessagingUtil.class, ConfiguredResourceAdapterNameTestCase.class)
                .addPackage(JMSOperations.class.getPackage())
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF")
                .addAsManifestResource("ejb/mdb/configuredresourceadapter/jboss-ejb3.xml", "jboss-ejb3.xml");
        logger.info(ejbJar.toString(true));
        return ejbJar;
    }

    @BeforeClass
    public static void createJmsDestinations() {
        jmsAdminOperations = JMSOperationsProvider.getInstance();
        jmsAdminOperations.createJmsQueue("override-resource-adapter-name-test/queue", QUEUE_JNDI_NAME);
        jmsAdminOperations.createJmsQueue("override-resource-adapter-name-test/reply-queue", REPLY_QUEUE_JNDI_NAME);
    }

    @AfterClass
    public static void afterTestClass() {
        if (jmsAdminOperations != null) {
            jmsAdminOperations.removeJmsQueue("override-resource-adapter-name-test/queue");
            jmsAdminOperations.removeJmsQueue("override-resource-adapter-name-test/reply-queue");
            jmsAdminOperations.close();
        }
    }

    @Test
    public void testMDBWithOverriddenResourceAdapterName() throws Exception {
        final String goodMorning = "Hello World";
        // send as TextMessage
        this.jmsUtil.sendTextMessage(goodMorning, this.queue, this.replyQueue);
        // wait for an reply
        final Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
        // test the reply
        final TextMessage textMessage = (TextMessage) reply;
        Assert.assertNotNull(textMessage);
        Assert.assertEquals("Unexpected reply message on reply queue: " + this.replyQueue, ConfiguredResourceAdapterNameMDB.REPLY, textMessage.getText());
    }

}
