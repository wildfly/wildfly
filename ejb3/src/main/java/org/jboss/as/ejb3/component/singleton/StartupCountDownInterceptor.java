package org.jboss.as.ejb3.component.singleton;


import org.jboss.as.ee.component.deployers.StartupCountdown;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;


/**
 * Interceptor decreasing value of passed CountDownLatch per invocation.
 * Is used on @Startup @Singletons' @PostConstruct methods to signal post-construct successfuly done.
 * @author Fedor Gavrilov
 */
public class StartupCountDownInterceptor implements Interceptor {
  private final StartupCountdown countdown;

  StartupCountDownInterceptor(final StartupCountdown countdown) {
    this.countdown = countdown;
  }

  @Override
  public Object processInvocation(final InterceptorContext context) throws Exception {
    final StartupCountdown.Frame frame = countdown.enter();
    try {
      return context.proceed();
    } finally {
      StartupCountdown.restore(frame);
      countdown.countDown();
    }
  }
}
