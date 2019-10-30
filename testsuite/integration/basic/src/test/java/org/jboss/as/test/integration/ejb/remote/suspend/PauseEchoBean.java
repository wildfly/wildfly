package org.jboss.as.test.integration.ejb.remote.suspend;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

/**
 * @author Stuart Douglas
 */
@Singleton
@Lock(LockType.READ)
public class PauseEchoBean implements Echo {

    private volatile CountDownLatch latch = null;


    @Override
    public String echo(String val) {
        if(latch == null) {
            latch = new CountDownLatch(1);
            try {
                latch.await(2, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            CountDownLatch l = latch;
            latch = null;
            l.countDown();
        }
        return val;
    }
}
