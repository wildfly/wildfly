package org.jboss.as.test.integration.ejb.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.ejb.Stateless;

@Stateless
public class AsyncChildBean extends AsyncParentClass {

    public void asyncMethod(CountDownLatch latch, CountDownLatch latch2) throws InterruptedException {
        latch.await(5, TimeUnit.SECONDS);
        voidMethodCalled = true;
        latch2.countDown();
    }
}
