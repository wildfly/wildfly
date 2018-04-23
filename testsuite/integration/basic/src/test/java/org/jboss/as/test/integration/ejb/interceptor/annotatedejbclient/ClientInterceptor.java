package org.jboss.as.test.integration.ejb.interceptor.annotatedejbclient;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

/**
 * Client side JBoss interceptor.
 */
public class ClientInterceptor implements EJBClientInterceptor {

  /**
   * Creates a new ClientInterceptor object.
   */
  public ClientInterceptor() {
  }

  @Override
  public void handleInvocation(EJBClientInvocationContext context) throws Exception {
      context.getContextData().put("ClientInterceptorInvoked", this.getClass().getName() + " " + context.getViewClass() + " " + context.getLocator());

      // count the number of times the interceptor is invoked
      if(context.getParameters().length > 0) {
          // String interceptorInvocationCounterKey = this.getClass().getName() + "-" + context.getViewClass() + "-" + context.getLocator() + "-" + context.getParameters()[0];
          String id = (String) context.getParameters()[0];
          String interceptorInvocationCounterKey = id + "-COUNT";
          Integer count = (Integer) context.getContextData().get(interceptorInvocationCounterKey);
          if(count == null) count = 0;
          count++;
          context.getContextData().put(interceptorInvocationCounterKey, count);
      }

      // Must make this call
      context.sendRequest();
  }

  @Override
  public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
      context.getContextData().get("ClientInterceptorInvoked");
      // Must make this call
      return context.getResult();
  }

}
