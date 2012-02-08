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

import java.net.InetAddress;
import java.security.Principal;
import java.util.Hashtable;

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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.common.JMSAdminOperations;
import org.jboss.as.test.integration.ejb.security.SecurityTest;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.logging.Logger;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.util.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
public class GetCallerPrincipalTestCase extends SecurityTest {

    private static final String QUEUE_NAME = "queue/callerPrincipal";

    private static final Logger log = Logger.getLogger(GetCallerPrincipalTestCase.class);

    private static final String ANONYMOUS = "anonymous"; //TODO: is this constant configured somewhere?
    private static final String OK = "OK";

    private static ModelControllerClient modelControllerClient;
    public static final String LOCAL_USER = "$local";

    @ArquillianResource
    Deployer deployer;


    @Deployment(managed=true, testable = false, name = "single", order = 0)
    public static Archive<?> deploymentSingleton()  {
        // to get things prepared before the deployment happens (@see  org.jboss.as.test.integration.ejb.security.AuthenticationTestCase)
        // and be sure that this will be called on first deployment (order=0) (btw. deployment without order has order == 0)
        try {
            createSecurityDomain();
        } catch (Exception e) {
            // ignore
        }

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar")
                .addClass(TestResultsSingleton.class)
                .addClass(ITestResultsSingleton.class)
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-single", "MANIFEST.MF");
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
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-bean", "MANIFEST.MF")                ;
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
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-bean", "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }

    @Deployment(managed=false, testable = false, name = "mdb", order = 102)
    public static Archive<?> deploymentMdb()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "mdb.jar")
                .addClass(MDBLifecycleCallback.class)
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-bean", "MANIFEST.MF")                ;
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
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        log.info(jar.toString(true));
        return jar;
    }

    @Deployment(managed = true, testable = true, name="test", order = 3)
    public static Archive<?> deployment()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "callerprincipal-test.jar")
                .addClass(GetCallerPrincipalTestCase.class)
                .addClass(Base64.class)
                .addClass(SecurityTest.class)
                .addClass(JMSAdminOperations.class)
                .addClass(SLSBWithoutSecurityDomain.class)
                .addClass(ISLSBWithoutSecurityDomain.class)
                .addClass(ShrinkWrapUtils.class)
                .addClass(PollingUtils.class)
                .addClass(EntityBean.class)
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-test", "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }

    @BeforeClass
    public static void init() throws Exception {
        modelControllerClient = ModelControllerClient.Factory.create(InetAddress.getByName("127.0.0.1"), 9999, getCallbackHandler());
        createQueue(modelControllerClient, "queue/callerPrincipal");
    }

    @AfterClass
    public static void clearUp() throws Exception {
        destroyQueue(modelControllerClient, "queue/callerPrincipal");
        modelControllerClient.close();
        removeSecurityDomain();
    }

    private InitialContext getInitialContext() throws NamingException {
        final Hashtable<String,String> jndiProperties = new Hashtable<String,String>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY,"org.jboss.as.naming.InitialContextFactory");
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(jndiProperties);
    }

    private static void createQueue(ModelControllerClient modelClient, String queueName) throws Exception {
        ModelNode addJmsQueue = getQueueAddr(queueName);
        addJmsQueue.get(ClientConstants.OP).set("add");
        addJmsQueue.get("entries").add("java:jboss/" + queueName);
        applyUpdate(addJmsQueue, modelClient);
    }

    private static void destroyQueue(ModelControllerClient modelClient, String queueName) throws Exception {
        ModelNode removeJmsQueue = getQueueAddr(queueName);
        removeJmsQueue.get(ClientConstants.OP).set("remove");
        applyUpdate(removeJmsQueue, modelClient);
    }

    private static ModelNode getQueueAddr(String name) {
        final ModelNode queueAddr = new ModelNode();
        queueAddr.get(ClientConstants.OP_ADDR).add("subsystem", "messaging");
        queueAddr.get(ClientConstants.OP_ADDR).add("hornetq-server", "default");
        queueAddr.get(ClientConstants.OP_ADDR).add("jms-queue", name);
        return queueAddr;
    }

    private ITestResultsSingleton getResultsSingleton() throws NamingException {
        return (ITestResultsSingleton) getInitialContext().lookup("ejb:/single//" + TestResultsSingleton.class.getSimpleName() + "!" + ITestResultsSingleton.class.getName());
    }

    private SecurityClient login() throws Exception {
        final SecurityClient client = SecurityClientFactory.getSecurityClient();
        client.setSimple("user1", "password1");
        client.login();
        return client;
    }


    /*
     * Tests
     */
    @Test
    public void testUnauthenticatedNoSecurityDomain() throws Exception {
        try {
            ISLSBWithoutSecurityDomain bean = (ISLSBWithoutSecurityDomain) getInitialContext().lookup("ejb:/callerprincipal-test//" + SLSBWithoutSecurityDomain.class.getSimpleName() + "!" + ISLSBWithoutSecurityDomain.class.getName());
            final Principal principal = bean.getCallerPrincipal();
            assertNotNull("EJB 3.1 FR 17.6.5 The container must never return a null from the getCallerPrincipal method.",
                    principal);
            assertEquals(ANONYMOUS, principal.getName());
        } catch (RuntimeException e) {
            e.printStackTrace();
            log.error(e.getStackTrace());
            fail("EJB 3.1 FR 17.6.5 The EJB container must provide the caller’s security context information during the execution of a business method ("
                    + e.getMessage() + ")");
        }
    }

    @Test
    public void testStatelessLifecycle() throws Exception {
        deployer.deploy("slsb");
        SecurityClient client = this.login();
        try {
            ITestResultsSingleton results = this.getResultsSingleton();
            IBeanLifecycleCallback bean = (IBeanLifecycleCallback) getInitialContext().lookup("ejb:/slsb//" + SLSBLifecycleCallback.class.getSimpleName() + "!" + IBeanLifecycleCallback.class.getName());
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
            IBeanLifecycleCallback bean = (IBeanLifecycleCallback) getInitialContext().lookup("ejb:/sfsb//" + SFSBLifecycleCallback.class.getSimpleName() + "!" + IBeanLifecycleCallback.class.getName() + "?stateful");
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
            QueueConnectionFactory qcf = (QueueConnectionFactory) new InitialContext().lookup("java:/RemoteConnectionFactory");
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
