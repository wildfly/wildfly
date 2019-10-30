package org.jboss.as.test.integration.ejb.stateless.pooling.ejb2;

import java.util.concurrent.atomic.AtomicInteger;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class CounterSingleton {
    public static AtomicInteger createCounter1 = new AtomicInteger(0);
    public static AtomicInteger createCounter2 = new AtomicInteger(0);
    public static AtomicInteger createCounter3 = new AtomicInteger(0);
    public static AtomicInteger removeCounter1 = new AtomicInteger(0);
    public static AtomicInteger removeCounter2 = new AtomicInteger(0);
    public static AtomicInteger removeCounter3 = new AtomicInteger(0);
}
