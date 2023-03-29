/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.remote.contextdata;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.logging.Logger;
import org.junit.Assert;

/**
 * Client side JBoss interceptor.
 */
public class ClientInterceptor implements EJBClientInterceptor {

  private static final Logger logger = Logger.getLogger(ClientInterceptor.class);

  /**
   * Creates a new ClientInterceptor object.
   */
  public ClientInterceptor() {
  }

  @Override
  public void handleInvocation(EJBClientInvocationContext context) throws Exception {
      logger.debugf("%s: ContextData: %s", "handleInvocation", context.getContextData());

      Assert.assertNotNull(context.getParameters());
      Assert.assertEquals(1, context.getParameters().length);
      Assert.assertTrue(context.getParameters()[0] instanceof UseCaseValidator);

      ((UseCaseValidator)context.getParameters()[0]).test(UseCaseValidator.InvocationPhase.CLIENT_INT_HANDLE_INVOCATION, context.getContextData());
      logger.debugf("%s: ContextData: %s", "handleInvocation", context.getContextData());

    // Must make this call
    context.sendRequest();
  }

  @Override
  public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
    UseCaseValidator useCaseValidator = (UseCaseValidator) context.getResult();
    Assert.assertNotNull(useCaseValidator);
    try {
        return useCaseValidator; //in case there was an exception
    } finally {
        logger.debugf("%s: ContextData: %s", "handleInvocationResult", context.getContextData());
        useCaseValidator.test(UseCaseValidator.InvocationPhase.CLIENT_INT_HANDLE_INVOCATION_RESULT, context.getContextData());
    }
  }
}