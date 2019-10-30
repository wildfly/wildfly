package org.jboss.as.test.multinode.ejb.timer.database;

import javax.ejb.Remote;
import java.util.List;

/**
 * @author Stuart Douglas
 */
@Remote
public interface Collector {

    void timerRun(final String nodeName, String info);

    List<TimerData> collect(final int expectedCount);

}
