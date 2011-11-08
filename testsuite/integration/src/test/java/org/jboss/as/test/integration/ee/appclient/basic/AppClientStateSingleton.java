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

    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void makeAppClientCall() {
        latch.countDown();
    }

    @Override
    public boolean awaitAppClientCall() {
        try {
            return latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
