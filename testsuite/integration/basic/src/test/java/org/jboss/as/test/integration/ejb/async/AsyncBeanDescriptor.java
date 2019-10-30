package org.jboss.as.test.integration.ejb.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;

public class AsyncBeanDescriptor {
    public static volatile boolean futureMethodCalled = false;

    public Future<Boolean> futureMethod(CountDownLatch latch) throws InterruptedException {
        futureMethodCalled = true;
        latch.countDown();
        return new AsyncResult<Boolean>(true);
    }
}
