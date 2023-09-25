/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.authorization;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.MessageDriven;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

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
