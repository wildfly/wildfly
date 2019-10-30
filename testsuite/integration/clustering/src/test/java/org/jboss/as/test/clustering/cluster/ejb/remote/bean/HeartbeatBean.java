package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import java.util.Date;

@Stateless
@Remote(Heartbeat.class)
public class HeartbeatBean implements Heartbeat {

    @Override
    public Result<Date> pulse() {
        Date now = new Date();
        return new Result<>(now);
    }
}
