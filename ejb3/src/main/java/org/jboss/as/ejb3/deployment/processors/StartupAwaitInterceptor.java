package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.deployers.StartupCountdown;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * Interceptor forcing invocation to wait until passed CountDownLatch value is decreased to 0.
 * Is used to suspend external requests to EJB methods until all startup beans in the deployment are started as per spec.
 * @author Fedor Gavrilov
 */
public class StartupAwaitInterceptor implements Interceptor {
  private final StartupCountdown countdown;

  StartupAwaitInterceptor(final StartupCountdown countdown) {
    this.countdown = countdown;
  }

  @Override
  public Object processInvocation(final InterceptorContext context) throws Exception {
    countdown.await();
    return context.proceed();
  }
}