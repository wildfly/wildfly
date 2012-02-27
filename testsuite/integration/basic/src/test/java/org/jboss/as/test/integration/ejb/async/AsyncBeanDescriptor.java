package org.jboss.as.test.integration.ejb.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.ejb.AsyncResult;

public class AsyncBeanDescriptor {
    public static boolean futureMethodCalled = false;
    
    public Future<Boolean> futureMethod(CountDownLatch latch) throws InterruptedException {
        latch.countDown();
        futureMethodCalled = true;
        return new AsyncResult<Boolean>(true);
    }
}
