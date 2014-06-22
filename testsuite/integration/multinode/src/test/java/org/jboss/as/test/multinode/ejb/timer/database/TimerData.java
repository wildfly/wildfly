package org.jboss.as.test.multinode.ejb.timer.database;

import java.io.Serializable;

/**
 * @author Stuart Douglas
 */
public class TimerData implements Serializable {

    private final String node;
    private final String info;

    public TimerData(String node, String info) {
        this.node = node;
        this.info = info;
    }

    public String getNode() {
        return node;
    }

    public String getInfo() {
        return info;
    }

    @Override
    public String toString() {
        return "{" +
                "node='" + node + '\'' +
                ", info='" + info + '\'' +
                '}';
    }
}
