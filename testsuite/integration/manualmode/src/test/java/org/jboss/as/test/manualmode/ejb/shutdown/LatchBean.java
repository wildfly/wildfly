package org.jboss.as.test.manualmode.ejb.shutdown;

import java.util.concurrent.CountDownLatch;

import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Singleton;

/**
 * @author Stuart Douglas
 */
@Singleton
@Remote(RemoteLatch.class)
public class LatchBean implements RemoteLatch {

    private static final CountDownLatch shutDownLatch = new CountDownLatch(1);
    private static final CountDownLatch messageLatch = new CountDownLatch(1);

    private String echoMessage;

    @Override
    @Asynchronous
    public void testDone() {
        shutDownLatch.countDown();
    }

    public static CountDownLatch getShutDownLatch() {
        return shutDownLatch;
    }

    public String getEchoMessage() {
        try {
            messageLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return echoMessage;
    }

    public void setEchoMessage(final String echoMessage) {
        this.echoMessage = echoMessage;
        messageLatch.countDown();
    }

}
