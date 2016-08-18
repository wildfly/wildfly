/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.PropertyPermission;
import javax.jms.Destination;
import javax.jms.JMSException;
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
      deployment.addAsResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
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
