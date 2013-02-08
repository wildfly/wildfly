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

package org.jboss.as.test.integration.ejb.security.callerprincipal;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.logging.Logger;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.util.Base64;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.ejb.EJBHome;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * The Bean Provider can invoke the getCallerPrincipal and isCallerInRole methods only
 * in the enterprise bean’s business methods as specified in Table 1 on page 94, Table 2 on page 103,
 * Table 4 on page 149, Table 5 on page 231, and Table 11 on page 303. If they are otherwise invoked
 * when no security context exists, they should throw the java.lang.IllegalStateException
 * runtime exception.
 *
 * In case of no security context
 * Stateless - PostConstruct, PreDestroy
 * MDB - PostConstruct, PreDestroy
 * Entity Beans - ejbActivate, ebjPassivate
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@ServerSetup({EjbSecurityDomainSetup.class, GetCallerPrincipalTestCase.JmsQueueSetup.class})
@Category(CommonCriteria.class)
public class GetCallerPrincipalTestCase {

    private static final String QUEUE_NAME = "queue/callerPrincipal";

    private static final Logger log = Logger.getLogger(GetCallerPrincipalTestCase.class);

    private static final String ANONYMOUS = "anonymous"; //TODO: is this constant configured somewhere?
    private static final String OK = "OK";

    public static final String LOCAL_USER = "$local";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    private InitialContext initialContext;

    static class JmsQueueSetup extends AbstractMgmtServerSetupTask {

        @Override
        protected void doSetup(final ManagementClient managementClient) throws Exception {
            createQueue(QUEUE_NAME);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            destroyQueue(QUEUE_NAME);
        }
        private void createQueue(String queueName) throws Exception {
            ModelNode addJmsQueue = getQueueAddr(queueName);
            addJmsQueue.get(ClientConstants.OP).set("add");
            addJmsQueue.get("entries").add("java:jboss/" + queueName);
            executeOperation(addJmsQueue);
        }

        private void destroyQueue(String queueName) throws Exception {
            ModelNode removeJmsQueue = getQueueAddr(queueName);
            removeJmsQueue.get(ClientConstants.OP).set("remove");
            executeOperation(removeJmsQueue);
        }
    }


    @Deployment(managed=true, testable = false, name = "single", order = 0)
    public static Archive<?> deploymentSingleton()  {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar")
                .addClass(TestResultsSingleton.class)
                .addClass(ITestResultsSingleton.class)
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-single", "MANIFEST.MF");
        jar.addPackage(CommonCriteria.class.getPackage());
        log.info(jar.toString(true));
        return jar;
    }

    @Deployment(managed=false, testable = false, name = "slsb", order = 100)
    public static Archive<?> deploymentSlsb()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "slsb.jar")
                .addClass(SLSBLifecycleCallback.class)
                .addClass(IBeanLifecycleCallback.class)
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-bean", "MANIFEST.MF");
        jar.addPackage(CommonCriteria.class.getPackage());
        log.info(jar.toString(true));
        return jar;
    }

    @Deployment(managed=false, testable = false, name = "sfsb", order = 101)
    public static Archive<?> deploymentSfsb()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "sfsb.jar")
                .addClass(SFSBLifecycleCallback.class)
                .addClass(IBeanLifecycleCallback.class)
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-bean", "MANIFEST.MF");
        jar.addPackage(CommonCriteria.class.getPackage());
        log.info(jar.toString(true));
        return jar;
    }

    @Deployment(managed=false, testable = false, name = "mdb", order = 102)
    public static Archive<?> deploymentMdb()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "mdb.jar")
                .addClass(MDBLifecycleCallback.class)
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-bean", "MANIFEST.MF")                ;
        jar.addPackage(CommonCriteria.class.getPackage());
        log.info(jar.toString(true));
        return jar;
    }

    @Deployment(managed=false, testable = false, name = "eb", order = 103)
    public static Archive<?> deploymentEntityBean()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "eb.jar")
                .addClass(EntityBeanBean.class)
                .addClass(EntityBeanHome.class)
                .addClass(EntityBean.class)
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-bean", "MANIFEST.MF")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addPackage(CommonCriteria.class.getPackage());
        log.info(jar.toString(true));
        return jar;
    }

    @Deployment(managed = true, testable = true, name="test", order = 3)
    public static Archive<?> deployment()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "callerprincipal-test.jar")
                .addClass(GetCallerPrincipalTestCase.class)
                .addClass(Base64.class)
                .addClass(SLSBWithoutSecurityDomain.class)
                .addClass(ISLSBWithoutSecurityDomain.class)
                .addClass(PollingUtils.class)
                .addClass(EntityBean.class)
                .addClasses(JmsQueueSetup.class, EjbSecurityDomainSetup.class, AbstractSecurityDomainSetup.class, AbstractMgmtTestBase.class)
                .addPackage(AbstractMgmtTestBase.class.getPackage()).addClasses(MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class)
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-test", "MANIFEST.MF");
        jar.addPackage(CommonCriteria.class.getPackage());
        log.info(jar.toString(true));
        return jar;
    }

    private static ModelNode getQueueAddr(String name) {
        final ModelNode queueAddr = new ModelNode();
        queueAddr.get(ClientConstants.OP_ADDR).add("subsystem", "messaging");
        queueAddr.get(ClientConstants.OP_ADDR).add("hornetq-server", "default");
        queueAddr.get(ClientConstants.OP_ADDR).add("jms-queue", name);
        return queueAddr;
    }

    private ITestResultsSingleton getResultsSingleton() throws NamingException {
        return (ITestResultsSingleton) initialContext.lookup("ejb:/single//" + TestResultsSingleton.class.getSimpleName() + "!" + ITestResultsSingleton.class.getName());
    }

    private SecurityClient login() throws Exception {
        final SecurityClient client = SecurityClientFactory.getSecurityClient();
        client.setSimple("user1", "password1");
        client.login();
        return client;
    }

    @Test
    public void testStatelessLifecycle() throws Exception {
        deployer.deploy("slsb");
        SecurityClient client = this.login();
        try {
            ITestResultsSingleton results = this.getResultsSingleton();
            IBeanLifecycleCallback bean = (IBeanLifecycleCallback) initialContext.lookup("ejb:/slsb//" + SLSBLifecycleCallback.class.getSimpleName() + "!" + IBeanLifecycleCallback.class.getName());
            log.debug("Stateless bean returns: " + bean.get());

            Assert.assertEquals(OK + "start", results.getSlsb("postconstruct"));

            deployer.undeploy("slsb");

            Assert.assertEquals(OK + "stop", results.getSlsb("predestroy"));
        } finally {
            client.logout();
        }
    }

    @Test
    public void testStatefulLifecycle() throws Exception {
        deployer.deploy("sfsb");
        SecurityClient client = this.login();
        ITestResultsSingleton results = this.getResultsSingleton();
        try {
            IBeanLifecycleCallback bean = (IBeanLifecycleCallback) initialContext.lookup("ejb:/sfsb//" + SFSBLifecycleCallback.class.getSimpleName() + "!" + IBeanLifecycleCallback.class.getName() + "?stateful");
            log.debug("Stateful bean returns: " + bean.get());

            Assert.assertEquals(ANONYMOUS + "start", results.getSfsb("postconstruct"));

            bean.remove();

            Assert.assertEquals(LOCAL_USER +  "stop", results.getSfsb("predestroy"));
        } finally {
            deployer.undeploy("sfsb");
            client.logout();
        }
    }

    /**
     * Run this one in the container so it can lookup the queue
     * @throws Exception
     */
    @OperateOnDeployment("test")
    @Test
    public void testMDBLifecycle() throws Exception {
        deployer.deploy("mdb");
        SecurityClient client = this.login();
        ITestResultsSingleton results = this.getResultsSingleton();

        MessageProducer producer = null;
        MessageConsumer consumer = null;
        QueueConnection conn = null;
        Session session = null;

        try {
            QueueConnectionFactory qcf = (QueueConnectionFactory) new InitialContext().lookup("java:/ConnectionFactory");
            Queue queue = (Queue) new InitialContext().lookup("java:jboss/" + QUEUE_NAME);

            conn = qcf.createQueueConnection("guest", "guest");
            conn.start();
            session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

            TemporaryQueue replyQueue = session.createTemporaryQueue();

            TextMessage msg = session.createTextMessage("Hello world");
            msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
            msg.setJMSReplyTo(replyQueue);

            producer = session.createProducer(queue);
            producer.send(msg);
            consumer = session.createConsumer(replyQueue);
            Message replyMsg = consumer.receive(5000);

            Object obj = ((ObjectMessage) replyMsg).getObject();
            log.info("MDB message get: " + obj);

            Assert.assertEquals(OK + "start", results.getMdb("postconstruct"));

            deployer.undeploy("mdb");

            Assert.assertEquals(OK + "stop", results.getMdb("predestroy"));
        } finally {
            if(consumer != null) {
                consumer.close();
            }
            if(producer!=null) {
                producer.close();
            }
            if(session!=null) {
                session.close();
            }
            if(conn!=null) {
                conn.stop();
            }
            client.logout();
        }
    }

    @Test
    public void testEBActivate() throws Exception {
        deployer.deploy("eb");
        EntityBean eb = setUpEB();
        SecurityClient client = this.login();
        ITestResultsSingleton results = this.getResultsSingleton();

        try {
            Assert.assertEquals(OK, results.getEb("ejbactivate"));
        } finally {
            tearDownEB(eb);
            client.logout();
        }
        deployer.undeploy("eb");
    }

    // app name: simple jar - empty app name
    // module name: name of jar = eb
    private <T extends EJBHome> T getHome(final Class<T> homeClass, final String beanName) {
        final EJBHomeLocator<T> locator = new EJBHomeLocator<T>(homeClass, "", "eb", beanName, "");
        return EJBClient.createProxy(locator);
    }

    private EntityBean setUpEB() throws Exception {
        EntityBeanHome ebHome = getHome(EntityBeanHome.class, "EntityBeanCallerPrincipal");
        EntityBean entityBean = null;

        try {
            entityBean = ebHome.findByPrimaryKey("test");
        } catch (Exception e) {
        }

        if (entityBean == null) {
            entityBean = ebHome.create("test");
        }
        return entityBean;
    }

    private void tearDownEB(EntityBean eb) throws Exception {
        try {
            eb.remove();
        } catch(Exception e) {
            // ;)
        }
    }
}
