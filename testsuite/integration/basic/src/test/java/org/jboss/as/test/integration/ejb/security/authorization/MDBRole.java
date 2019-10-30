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

package org.jboss.as.test.integration.ejb.security.authorization;

import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.ejb.MessageDriven;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */


@MessageDriven(activationConfig = {
   @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/myAwesomeQueue")
})
@RunAs("MDBrole")
@SecurityDomain(value = "ejb3-tests", unauthenticatedPrincipal = "nobody")
public class MDBRole implements MessageListener {
   @Resource(lookup = "java:/JmsXA")
   private ConnectionFactory factory;

   @EJB
   Simple simple;

   private String createResponse(){
      String response;
      try{
         response = simple.testAuthorizedRole();
      } catch (EJBAccessException ex) {
         response = "UNAUTHORIZED: testAuthorizedRole()";
         return response;
      }

      try{
         response = simple.testUnauthorizedRole();
         return "ILLEGALY AUTHORIZED: testUnauthorizedRole()";
      } catch (EJBAccessException ex) {
         // OK, we expect an exception
      }

      try{
         response = simple.testPermitAll();
      } catch (EJBAccessException ex) {
         response = "UNAUTHORIZED: testPermitAll()";
         return response;
      }

      try{
         response = simple.testDenyAll();
         return "ILLEGALY AUTHORIZED: testDenyAll()";
      } catch (EJBAccessException ex) {
         // OK, we expect an exception
      }


      return response;
   }

   public void onMessage(Message message) {
      try {
         if (!"Let's test it!".equals(((TextMessage) message).getText())){
            throw new AssertionError("Unexpected message: " + ((TextMessage) message).getText() + " ; expected:\"Let's test it!\"");
         }
         final Destination replyTo = message.getJMSReplyTo();
         // ignore messages that need no reply
         if (replyTo == null)
            return;
         try (
                JMSContext context = factory.createContext()
         ) {
            context.createProducer()
                  .setJMSCorrelationID(message.getJMSMessageID())
                  .send(replyTo, createResponse());
          }
      } catch (JMSException e) {
         throw new RuntimeException(e);
      }
   }
}
