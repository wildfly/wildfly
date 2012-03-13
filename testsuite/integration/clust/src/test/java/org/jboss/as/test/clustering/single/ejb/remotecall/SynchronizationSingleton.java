package org.jboss.as.test.clustering.single.ejb.remotecall;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * @author Ondrej Chaloupka
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SynchronizationSingleton implements SynchronizationSingletonInterface {
    private static final int WAITING_S = 5;
    private transient CountDownLatch latch;
    private transient CountDownLatch latch2;

    public void resetLatches() {
        latch = new CountDownLatch(1);
        latch2 = new CountDownLatch(2);
    }

    public void countDownLatchNumber1() {
        latch.countDown();
    }

    public void countDownLatchNumber2() {
        latch2.countDown();
    }

    public void waitForLatchNumber1() throws InterruptedException {
        latch.await(WAITING_S, TimeUnit.SECONDS);
    }

    public void waitForLatchNumber2() throws InterruptedException {
        latch2.await(WAITING_S, TimeUnit.SECONDS);
    }
}
