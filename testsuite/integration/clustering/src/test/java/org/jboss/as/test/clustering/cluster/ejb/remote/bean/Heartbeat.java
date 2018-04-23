package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import java.util.Date;

public interface Heartbeat {
    Result<Date> pulse();
}
