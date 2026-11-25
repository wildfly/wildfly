/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.PropertyPermission;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueReceiver;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.authorization.MDBRole;
import org.jboss.as.test.integration.ejb.security.authorization.Simple;
import org.jboss.as.test.integration.ejb.security.authorization.SimpleSLSB;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.permission.ChangeRoleMapperPermission;
import org.wildfly.security.permission.ElytronPermission;

/**
 * Deploys a message driven bean and a stateless bean which is injected into MDB
 * Then the cooperation of @RunAs annotation on MDB and @RolesAllowed annotation on SLSB is tested.
 * <p/>
 * https://issues.jboss.org/browse/JBQA-5628
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@RunWith(Arquillian.class)
@ServerSetup({CreateQueueSetupTask.class, EjbSecurityDomainSetup.class})
@Category(CommonCriteria.class)
public class MDBRoleTestCase {

   Logger logger = Logger.getLogger(MDBRoleTestCase.class);

   @Deployment
   public static Archive<?> deployment() {
      final JavaArchive deployment = ShrinkWrap.create(JavaArchive.class, "ejb3mdb.jar")
         .addClass(MDBRole.class)
         .addClass(CreateQueueSetupTask.class)
         .addClasses(AbstractSecurityDomainSetup.class, EjbSecurityDomainSetup.class)
         .addClass(Simple.class)
         .addClass(SimpleSLSB.class)
         .addClass(TimeoutUtil.class);
        deployment.addAsManifestResource(MDBRoleTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
      deployment.addPackage(CommonCriteria.class.getPackage());
      // grant necessary permissions
      // TODO WFLY-15289 The Elytron permissions need to be checked, should a deployment really need these?
      deployment.addAsResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read"),
                                                         new ElytronPermission("setRunAsPrincipal"),
                                                         new ChangeRoleMapperPermission("ejb")), "META-INF/jboss-permissions.xml");
      return deployment;
   }

   @Test
   public void testIsMDBinRole() throws NamingException, JMSException {

      final InitialContext ctx = new InitialContext();
      final QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("java:/JmsXA");
      final QueueConnection connection = factory.createQueueConnection();
      connection.start();

      final QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
      final Queue replyDestination = session.createTemporaryQueue();
      final QueueReceiver receiver = session.createReceiver(replyDestination);
      final Message message = session.createTextMessage("Let's test it!");
      message.setJMSReplyTo(replyDestination);
      final Destination destination = (Destination) ctx.lookup("queue/myAwesomeQueue");
      final MessageProducer producer = session.createProducer(destination);
      producer.send(message);
      producer.close();

      final Message reply = receiver.receive(TimeoutUtil.adjust(5000));
      assertNotNull(reply);
      final String result = ((TextMessage) reply).getText();
      assertEquals(SimpleSLSB.SUCCESS, result);
      connection.close();
   }

}
