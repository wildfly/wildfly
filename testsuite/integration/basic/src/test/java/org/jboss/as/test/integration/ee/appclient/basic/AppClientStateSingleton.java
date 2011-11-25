package org.jboss.as.test.integration.ee.appclient.basic;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;

/**
 * @author Stuart Douglas
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class AppClientStateSingleton implements AppClientSingletonRemote {

    private volatile CountDownLatch latch = new CountDownLatch(1);

    private volatile String value;

    @Override
    public void reset() {
        value = null;
        //if we have a thread blocked on the latch release it
        latch.countDown();
        latch = new CountDownLatch(1);
    }

    @Override
    public void makeAppClientCall(final String value) {
        this.value = value;
        latch.countDown();
    }

    @Override
    public String awaitAppClientCall() {
        try {
            latch.await(10, TimeUnit.SECONDS);
            return value;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
