package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import java.util.concurrent.TimeUnit;

import javax.ejb.Remote;
import javax.ejb.Stateless;
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
