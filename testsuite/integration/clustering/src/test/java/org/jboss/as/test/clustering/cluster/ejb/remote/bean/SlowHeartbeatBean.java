package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import java.util.concurrent.TimeUnit;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import java.util.Date;

@Stateless
@Remote(Heartbeat.class)
public class SlowHeartbeatBean implements Heartbeat {

    @Override
    public Result<Date> pulse() {
        delay();
        Date now = new Date();
        return new Result<>(now);
    }

    private static void delay() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
