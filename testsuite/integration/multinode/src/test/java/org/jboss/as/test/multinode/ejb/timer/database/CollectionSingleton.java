package org.jboss.as.test.multinode.ejb.timer.database;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author Stuart Douglas
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class CollectionSingleton implements Collector {

    private final LinkedBlockingDeque<TimerData> timerDatas = new LinkedBlockingDeque<>();

    @Override
    public void timerRun(String nodeName, String info) {
        timerDatas.add(new TimerData(nodeName, info));
    }

    @Override
    public List<TimerData> collect(int expectedCount) {
        long end = System.currentTimeMillis() + 30000; //10 seconds
        List<TimerData> ret = new ArrayList<>();
        for (; ; ) {
            if (ret.size() == expectedCount) {
                break;
            }
            if (System.currentTimeMillis() > end) {
                break;
            }
            try {
                TimerData res = timerDatas.poll(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                if (res != null) {
                    ret.add(res);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return ret;
    }
}
