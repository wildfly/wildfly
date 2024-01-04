/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.weld.asyncobserver;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@ApplicationScoped
public class TestObserver {

   public void recieve(@ObservesAsync TestEvent event) {
      try {
         InitialContext initialContext = new InitialContext();
         initialContext.lookup("java:comp/TransactionSynchronizationRegistry");
      } catch (NamingException e) {
         throw new RuntimeException(e);
      }
   }

   public static class TestEvent {

   }
}
