/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.app;

import java.io.Serializable;
import java.time.Instant;

public class TimerRecord {

    private String nodeName;
    private String className;
    private Serializable info;
    private boolean persistent;
    private Instant now;

    public TimerRecord(String nodeName, String className, Serializable info, boolean persistent, Instant now) {
        this.nodeName = nodeName;
        this.className = className;
        this.info = info;
        this.persistent = persistent;
        this.now = now;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Serializable getInfo() {
        return info;
    }

    public void setInfo(Serializable info) {
        this.info = info;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public Instant getNow() {
        return now;
    }

    public void setNow(Instant now) {
        this.now = now;
    }

    @Override
    public String toString() {
        return "TimerRecord [nodeName=" + nodeName + ", className=" + className + ", info=" + info + ", persistent="
                + persistent + ", now=" + now + "]";
    }

}
