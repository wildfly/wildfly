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
package org.jboss.as.webservices.invocation;

import javax.xml.ws.WebServiceContext;

import org.jboss.invocation.InterceptorContext;
import org.jboss.ws.common.injection.ThreadLocalAwareWebServiceContext;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.invocation.InvocationContext;

/**
 * Handles invocations on EJB3 endpoints.
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
