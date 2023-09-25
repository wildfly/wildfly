/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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