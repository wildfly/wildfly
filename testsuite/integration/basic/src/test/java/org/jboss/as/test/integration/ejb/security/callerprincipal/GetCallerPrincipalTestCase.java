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

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.concurrent.Callable;

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

import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

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
    private static final String QUEUE_LOOKUP = "java:jboss/" + QUEUE_NAME;

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
            JMSOperationsProvider.getInstance(managementClient).createJmsQueue(QUEUE_NAME, QUEUE_LOOKUP);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            JMSOperationsProvider.getInstance(managementClient).removeJmsQueue(QUEUE_NAME);
        }
    }


    @Deployment(managed=true, testable = false, name = "single", order = 0)
    public static Archive<?> deploymentSingleton()  {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar")
                .addClass(TestResultsSingleton.class)
                .addClass(ITestResultsSingleton.class)
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-single", "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain")), "permissions.xml");
        jar.addPackage(CommonCriteria.class.getPackage());
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
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-bean", "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain")), "permissions.xml");
        jar.addPackage(CommonCriteria.class.getPackage());
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
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-bean", "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain")), "jboss-permissions.xml");
        jar.addPackage(CommonCriteria.class.getPackage());
        return jar;
    }

    @Deployment(managed=false, testable = false, name = "mdb", order = 102)
    public static Archive<?> deploymentMdb()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "mdb.jar")
                .addClass(MDBLifecycleCallback.class)
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-bean", "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain")), "permissions.xml");
        jar.addPackage(CommonCriteria.class.getPackage());
        return jar;
    }

    @Deployment(managed = true, testable = true, name="test", order = 3)
    public static Archive<?> deployment()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "callerprincipal-test.jar")
                .addClass(GetCallerPrincipalTestCase.class)
                .addClass(SLSBWithoutSecurityDomain.class)
                .addClass(ISLSBWithoutSecurityDomain.class)
                .addClass(PollingUtils.class)
                .addClass(Util.class)
                .addClasses(JmsQueueSetup.class, EjbSecurityDomainSetup.class, AbstractSecurityDomainSetup.class, AbstractMgmtTestBase.class)
                .addPackage(AbstractMgmtTestBase.class.getPackage()).addClasses(MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class)
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(GetCallerPrincipalTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "MANIFEST.MF-test", "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain")), "permissions.xml");
        jar.addPackage(CommonCriteria.class.getPackage());
        return jar;
    }

    private ITestResultsSingleton getResultsSingleton() throws NamingException {
        return (ITestResultsSingleton) initialContext.lookup("ejb:/single//" + TestResultsSingleton.class.getSimpleName() + "!" + ITestResultsSingleton.class.getName());
    }

    @Test
    public void testStatelessLifecycle() throws Exception {
        deployer.deploy("slsb");
        final Callable<Void> callable = () -> {
            ITestResultsSingleton results = this.getResultsSingleton();
            IBeanLifecycleCallback bean = (IBeanLifecycleCallback) initialContext.lookup("ejb:/slsb//" + SLSBLifecycleCallback.class.getSimpleName() + "!" + IBeanLifecycleCallback.class.getName());
            log.trace("Stateless bean returns: " + bean.get());

            Assert.assertEquals(OK + "start", results.getSlsb("postconstruct"));

            deployer.undeploy("slsb");

            Assert.assertEquals(OK + "stop", results.getSlsb("predestroy"));
            return null;
        };
        Util.switchIdentitySCF("user1", "password1", callable);
    }

    @Test
    public void testStatefulLifecycle() throws Exception {
        deployer.deploy("sfsb");
        final Callable<Void> callable = () -> {
            ITestResultsSingleton results = this.getResultsSingleton();
            IBeanLifecycleCallback bean = (IBeanLifecycleCallback) initialContext.lookup("ejb:/sfsb//" + SFSBLifecycleCallback.class.getSimpleName() + "!" + IBeanLifecycleCallback.class.getName() + "?stateful");
            log.trace("Stateful bean returns: " + bean.get());

            Assert.assertEquals(ANONYMOUS + "start", results.getSfsb("postconstruct"));

            bean.remove();

            Assert.assertEquals(LOCAL_USER +  "stop", results.getSfsb("predestroy"));
            return null;
        };
        try {
            Util.switchIdentitySCF("user1", "password1", callable);
        } finally {
            deployer.undeploy("sfsb");
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
        final Callable<Void> callable = () -> {
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
                log.trace("MDB message get: " + obj);

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
                    conn.close();
                }
            }
            return null;
        };
        Util.switchIdentitySCF("user1", "password1", callable);
    }

}
