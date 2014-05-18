package org.jboss.as.test.multinode.ejb.timer.database;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface RemoteTimedBean {

    void scheduleTimer(long date, String info);

    boolean hasTimerRun();
}
