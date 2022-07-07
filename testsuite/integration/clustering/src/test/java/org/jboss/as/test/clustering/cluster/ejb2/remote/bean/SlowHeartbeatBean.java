package org.jboss.as.test.clustering.cluster.ejb2.remote.bean;

import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionBean;
import jakarta.ejb.Stateless;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Stateless
@Remote(HeartbeatRemote.class)
@RemoteHome(HeartbeatRemoteHome.class)
public class SlowHeartbeatBean extends HeartbeatBeanBase implements SessionBean {

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
