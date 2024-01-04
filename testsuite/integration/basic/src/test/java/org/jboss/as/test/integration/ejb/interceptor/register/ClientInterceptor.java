/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.register;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.logging.Logger;


/**
 * Client side JBoss interceptor.
 */
public class ClientInterceptor implements EJBClientInterceptor {

  private Logger log = Logger.getLogger(ClientInterceptor.class.getName());

  /**
   * Creates a new ClientInterceptor object.
   */
  public ClientInterceptor() {
  }

  @Override
  public void handleInvocation(EJBClientInvocationContext context) throws Exception {
    log.debug("In the client interceptor handleInvocation : " + this.getClass().getName() + " " + context.getViewClass() + " " + context.getLocator());

    // Must make this call
    context.sendRequest();
  }

  @Override
  public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
    log.debug("In the client interceptor handleInvocationResult : " + this.getClass().getName() + " " + context.getViewClass() + " " + context.getLocator());

    // Append some string to start of result to indicate the ClientInterceptor was invoked
    String result = RegisterInterceptorViaMetaFileTest.clientInterceptorPrefix + context.getResult();
    return result;
  }

}
