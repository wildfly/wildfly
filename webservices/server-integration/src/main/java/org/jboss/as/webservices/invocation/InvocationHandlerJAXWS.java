/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.invocation;

import jakarta.xml.ws.WebServiceContext;

import org.jboss.invocation.InterceptorContext;
import org.jboss.ws.common.injection.ThreadLocalAwareWebServiceContext;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.invocation.InvocationContext;

/**
 * Handles invocations on Enterprise Beans 3 endpoints.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class InvocationHandlerJAXWS extends AbstractInvocationHandler {

   @Override
   public void onBeforeInvocation(final Invocation invocation) {
      final WebServiceContext wsContext = getWebServiceContext(invocation);
      ThreadLocalAwareWebServiceContext.getInstance().setMessageContext(wsContext);
   }

   @Override
   public void onAfterInvocation(final Invocation invocation) {
      ThreadLocalAwareWebServiceContext.getInstance().setMessageContext(null);
   }

   @Override
   protected void prepareForInvocation(final InterceptorContext context, final Invocation wsInvocation) {
       context.setContextData(getWebServiceContext(wsInvocation).getMessageContext());
   }

   private static WebServiceContext getWebServiceContext(final Invocation invocation) {
      final InvocationContext invocationContext = invocation.getInvocationContext();
      return invocationContext.getAttachment(WebServiceContext.class);
   }

}
