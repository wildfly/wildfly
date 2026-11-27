/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.weld.asyncobserver;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class AsyncObserverAccessCompNamespaceTestCase {

   @Deployment
   public static Archive<?> getDeployment() {
      JavaArchive lib = ShrinkWrap.create(JavaArchive.class)
         .addClasses(TestObserver.class, AsyncObserverAccessCompNamespaceTestCase.class);
      return lib;
   }

   @Inject
   Event<TestObserver.TestEvent> testEvents;

   @Test
   public void testSendingEvent() throws Exception {
      CompletableFuture<TestObserver.TestEvent> future = testEvents.fireAsync(new TestObserver.TestEvent()).toCompletableFuture();

      // just a sanity check - should throw exception on future.get in case of problems
      Assertions.assertNotNull(future.get(500, TimeUnit.MILLISECONDS));
   }
}
